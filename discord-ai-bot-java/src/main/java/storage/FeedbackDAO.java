package storage;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FeedbackDAO {

    // INSERT: feedback_text + discord_id, stars = null
    public void insertFeedback(String feedbackText, long discordId) {
        String sql = "INSERT INTO feedback (feedback_text, discord_id, stars) VALUES (?, ?, NULL)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, feedbackText);
            stmt.setString(2, String.valueOf(discordId));

            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // UPDATE: only the latest feedback entry with null stars
    public void updateStarsByDiscordId(String discordId, int stars) {
        String sql = """
            UPDATE feedback
            SET stars = ?
            WHERE id = (
                SELECT id FROM feedback
                WHERE discord_id = ? AND stars IS NULL
                ORDER BY id DESC
                LIMIT 1
            )
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stars);
            stmt.setString(2, discordId);

            int rowsUpdated = stmt.executeUpdate();
            System.out.println("Rows updated: " + rowsUpdated);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
