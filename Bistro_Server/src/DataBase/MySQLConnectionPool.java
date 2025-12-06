package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Singleton connection pool ל-MySQL.
 *
 * לפי ההנחיות:
 * - לא סוגרים חיבורים אוטומטית אחרי 30 דקות.
 * - אם חיבור היה לא פעיל יותר מ-30 דקות, מסמנים אותו כ-expired.
 * - רק כאשר מתקבלת בקשה חדשה לחיבור, בודקים אם החיבור expired:
 *   אם כן – סוגרים אותו ופותחים חיבור חדש טרי.
 */
public class MySQLConnectionPool {

    // *** תעדכן כאן את ה-URL / USER / PASSWORD לפי המסד שלך ***
    private static final String URL      = "jdbc:mysql://localhost:3306/bistrodb"; // לדוגמה
    private static final String USER     = "root";
    private static final String PASSWORD = "Yazan12@";

    // כמה זמן חיבור יכול להיות ללא שימוש לפני שנחשיב אותו "ישן"
    private static final long MAX_IDLE_MILLIS =
            TimeUnit.MINUTES.toMillis(30);   // 30 דקות

    // גודל התחלתי של הבריכה וכמות מקסימלית (לא חובה לגעת)
    private static final int INITIAL_SIZE = 3;
    private static final int MAX_SIZE     = 10;

    private static MySQLConnectionPool instance;

    // חיבורים פנויים שאפשר לקחת מהם
    private final BlockingQueue<PooledConnection> availableConnections;

    // כל החיבורים שנוצרו אי פעם (כדי שנוכל למצוא ולסגור אותם ב-shutdown)
    private final Set<PooledConnection> allConnections;

    private MySQLConnectionPool() throws SQLException {
        this.availableConnections = new LinkedBlockingQueue<>();
        this.allConnections       = new HashSet<>();

        // יצירת כמה חיבורים התחלתיים
        for (int i = 0; i < INITIAL_SIZE; i++) {
            PooledConnection pConn = createNewPooledConnection();
            availableConnections.offer(pConn);
            allConnections.add(pConn);
        }
    }

    /**
     * Singleton – קבלת מופע יחיד של ה-pool.
     */
    public static synchronized MySQLConnectionPool getInstance() throws SQLException {
        if (instance == null) {
            instance = new MySQLConnectionPool();
        }
        return instance;
    }

    /**
     * יצירת חיבור חדש פיזית למסד והעטיפה שלו ב-PooledConnection.
     */
    private PooledConnection createNewPooledConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        return new PooledConnection(conn);
    }

    /**
     * קבלת חיבור לשימוש.
     *
     * הלוגיקה:
     * 1. מנסים לקחת חיבור מ-availableConnections.
     * 2. אם אין חיבור פנוי ויש מקום – יוצרים חדש.
     * 3. אם יש חיבור אבל הוא expired או סגור → סוגרים אותו ופותחים חדש.
     * 4. מסמנים markUsed ומחזירים את ה-Connection האמיתי.
     */
    public synchronized Connection getConnection() throws SQLException {
        PooledConnection pConn = availableConnections.poll();

        if (pConn == null) {
            // אין חיבור פנוי – אם לא עברנו את ה-max, ניצור חדש
            if (allConnections.size() < MAX_SIZE) {
                pConn = createNewPooledConnection();
                allConnections.add(pConn);
            } else {
                // אם עברנו max, נחכה עד שמישהו יחזיר חיבור
                try {
                    pConn = availableConnections.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for DB connection", e);
                }
            }
        } else {
            // יש חיבור – נבדוק אם הוא ישן מדי או סגור
            if (pConn.isExpired(MAX_IDLE_MILLIS) || pConn.isClosed()) {
                pConn.closePhysicalConnection();
                pConn = createNewPooledConnection();
                allConnections.add(pConn);
            }
        }

        // מסמנים שנעשה בו שימוש "עכשיו"
        pConn.markUsed();

        // מחזירים את החיבור האמיתי ל-DAO
        return pConn.getConnection();
    }

    /**
     * שחרור חיבור חזרה לבריכה.
     * חשוב: לא לקרוא conn.close() מבחוץ – אלא releaseConnection.
     */
    public synchronized void releaseConnection(Connection conn) {
        if (conn == null) {
            return;
        }

        // מוצאים את ה-PooledConnection שמתאים ל-Connection הזה
        PooledConnection target = null;
        for (PooledConnection pConn : allConnections) {
            if (pConn.getConnection() == conn) { // השוואת רפרנס
                target = pConn;
                break;
            }
        }

        if (target == null) {
            // לא מכירים את החיבור הזה – נסגור אותו פיזית ליתר ביטחון
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
            return;
        }

        // מסמנים שימוש אחרון ומחזירים לרשימת הזמינים
        target.markUsed();
        availableConnections.offer(target);
    }

    /**
     * סגירה מסודרת של כל החיבורים, למשל בכיבוי שרת.
     */
    public synchronized void shutdown() {
        for (PooledConnection pConn : allConnections) {
            pConn.closePhysicalConnection();
        }
        allConnections.clear();
        availableConnections.clear();
    }
}



