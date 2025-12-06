package DataBase;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * עטיפה לחיבור JDBC אמיתי + שמירת זמן שימוש אחרון.
 * ה-connection pool משתמש בזה כדי לדעת אם החיבור "ישן מדי" (expired).
 */
public class PooledConnection {

    // החיבור האמיתי ל-MySQL
    private final Connection conn;

    // הזמן האחרון (millis) שבו נעשה שימוש בחיבור
    private long lastUsed;

    public PooledConnection(Connection conn) {
        this.conn = conn;
        this.lastUsed = System.currentTimeMillis();
    }

    /**
     * מחזיר את החיבור האמיתי.
     */
    public Connection getConnection() {
        return conn;
    }

    /**
     * לסמן שנעשה שימוש בחיבור עכשיו.
     * כדאי לקרוא לזה כשמקבלים את החיבור מה-pool
     * וגם כשמחזירים אותו אחרי שימוש.
     */
    public void markUsed() {
        lastUsed = System.currentTimeMillis();
    }

    /**
     * האם החיבור "פג תוקף" מבחינת זמן (idle זמן רב מדי).
     *
     * @param maxIdleMillis כמה מילי-שניות מותר שלא יהיה שימוש
     * @return true אם עבר יותר מ-maxIdleMillis מאז השימוש האחרון
     */
    public boolean isExpired(long maxIdleMillis) {
        long now = System.currentTimeMillis();
        return (now - lastUsed) > maxIdleMillis;
    }

    /**
     * בדיקה אם החיבור כבר נסגר פיזית.
     */
    public boolean isClosed() {
        try {
            return conn.isClosed();
        } catch (SQLException e) {
            // במקרה של שגיאה נתייחס אליו כסגור
            return true;
        }
    }

    /**
     * סגירה פיזית של החיבור למסד.
     * רק ה-connection pool אמור לקרוא לזה.
     */
    public void closePhysicalConnection() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace(); // אפשר להחליף בלוג רציני יותר
        }
    }
}


