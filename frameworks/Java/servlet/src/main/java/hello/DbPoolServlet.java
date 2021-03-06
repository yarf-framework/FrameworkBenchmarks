package hello;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Database connectivity (with a Servlet-container managed pool) test.
 */
@SuppressWarnings("serial")
public class DbPoolServlet extends HttpServlet {
	// Database details.
	private static final String DB_QUERY = "SELECT * FROM World WHERE id = ?";
	private static final int DB_ROWS = 10000;
	private static final int LIMIT = DB_ROWS + 1;

	// Database connection pool.
	@Resource(name = "jdbc/hello_world")
	private DataSource dataSource;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException,
			IOException {
		// Reference the data source.
		final DataSource source = dataSource;
		final int count = Common.normalise(req.getParameter("queries"));
		final World[] worlds = new World[count];
		final Random random = ThreadLocalRandom.current();

		// Fetch some rows from the database.
		try (Connection conn = source.getConnection()) {
			try (PreparedStatement statement = conn.prepareStatement(DB_QUERY,
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				// Run the query the number of times requested.
				for (int i = 0; i < count; i++) {
					final int id = random.nextInt(LIMIT);
					statement.setInt(1, id);

					try (ResultSet results = statement.executeQuery()) {
						if (results.next()) {
							worlds[i] = new World(id, results.getInt("randomNumber"));
						}
					}
				}
			}
		} catch (SQLException sqlex) {
			throw new ServletException(sqlex);
		}

		// Set content type to JSON
		res.setHeader(Common.HEADER_CONTENT_TYPE, Common.CONTENT_TYPE_JSON);

		// Write JSON encoded message to the response.
		if (count == 1) {
			Common.MAPPER.writeValue(res.getOutputStream(), worlds[0]);
		} else {
			Common.MAPPER.writeValue(res.getOutputStream(), worlds);
		}
	}
}