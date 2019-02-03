package db;

import com.bot.db.GuildDAO;
import com.bot.models.InternalGuild;
import com.bot.models.UserMembership;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class GuildIT {

    private static Connection connection;
    private static Flyway flyway;
    private static GuildDAO guildDAO;
    private static final Logger LOGGER = Logger.getLogger(GuildIT.class.getName());

    private List<UserMembership> memberships = Arrays.asList(
            new UserMembership("1", "user-1", "101", true),
            new UserMembership("2", "user-2", "102", true),
            new UserMembership("3", "user-3", "101", true),
            new UserMembership("4", "user-4", "101", true),
            new UserMembership("3", "user-3", "102", false)
            );

    private List<InternalGuild> guilds = Arrays.asList(
            new InternalGuild("101", "guild-1", 100, "1", "2", "2", "1"),
            new InternalGuild("102", "guild-2", 100, "2", "2", "2", "3")
    );

    @BeforeClass
    public static void setUpConnections() throws SQLException, InterruptedException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://localhost:1337/testdb");
        hikariConfig.setUsername("mysql");
        hikariConfig.setPassword("mysql");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        // The integration tests start too fast (OH NOOOO) So we might need to wait for a couple seconds to ensure the db is up.
        HikariDataSource dataSource;
        int count = 0;
        int maxTries = 10;
        while(true) {
            int waitTime = ++count * 1000;
            try {
                Thread.sleep(waitTime);
                dataSource = new HikariDataSource(hikariConfig);
                break;
            } catch (HikariPool.PoolInitializationException e) {
                LOGGER.log(Level.WARNING, "Failed to connect to db, waiting for " + waitTime + " ms");
                if (count == maxTries) throw e;
            }
        }

        connection = dataSource.getConnection();

        flyway = new Flyway();
        flyway.setDataSource(dataSource);

        guildDAO = new GuildDAO(connection);
    }

    @Before
    public void setUp() throws SQLException {
        resetdb();
        loadGuilds();
        loadUsers();
    }

    private void resetdb() {
        flyway.clean();
        flyway.migrate();
    }

    private void loadGuilds() throws SQLException {
        String query = "INSERT INTO guild(id, name, default_volume, min_base_role_id, min_mod_role_id, min_voice_role_id, min_nsfw_role_id) VALUES(?,?,?,?,?,?,?)";
        PreparedStatement statement = connection.prepareStatement(query);

        for(InternalGuild g : guilds) {
            statement.setString(1, g.getId());
            statement.setString(2, g.getName());
            statement.setInt(3, g.getVolume());
            statement.setString(4, g.getMinBaseRole());
            statement.setString(5, g.getMinModRole());
            statement.setString(6, g.getMinVoiceRole());
            statement.setString(7, g.getMinNsfwRole());

            statement.addBatch();
        }

        statement.executeBatch();
        statement.close();
    }

    private void loadUsers() throws SQLException {
        String userQuery = "INSERT INTO users(id, name) VALUES(?,?) ON DUPLICATE KEY UPDATE name=name";
        String membershipQuery = "INSERT INTO guild_membership(guild, user_id, can_use_bot) VALUES(?,?,?)";
        PreparedStatement userStatement = connection.prepareStatement(userQuery);
        PreparedStatement membershipStatement = connection.prepareStatement(membershipQuery);

        for (UserMembership u : memberships) {
            userStatement.setString(1, u.getId());
            userStatement.setString(2, u.getName());

            userStatement.addBatch();

            membershipStatement.setString(1, u.getGuildId());
            membershipStatement.setString(2, u.getId());
            membershipStatement.setBoolean(3, u.isCanUseBot());

            membershipStatement.addBatch();
        }

        userStatement.executeBatch();
        userStatement.close();
        membershipStatement.executeBatch();
        membershipStatement.close();
    }

    private void assertGuildEquals(InternalGuild expected, InternalGuild returned) {
        assertEquals(expected.getId(), returned.getId());
        assertEquals(expected.getName(), returned.getName());
        assertEquals(expected.getVolume(), returned.getVolume());
        assertEquals(expected.getMinBaseRole(), returned.getMinBaseRole());
        assertEquals(expected.getMinModRole(), returned.getMinModRole());
        assertEquals(expected.getMinNsfwRole(), returned.getMinNsfwRole());
        assertEquals(expected.getMinVoiceRole(), returned.getMinVoiceRole());
    }

    @Test
    public void testGetGuildById() throws SQLException {
        InternalGuild expected = guilds.get(1); // Guild 102
        InternalGuild returned = guildDAO.getGuildById(expected.getId());

        assertGuildEquals(expected, returned);
    }

    @Test
    public void testAddGuild() throws SQLException {
        InternalGuild expected = new InternalGuild("999", "test-added", 100, "999", "12345", "999", "999");
        Role lowRole = mock(Role.class);
        Role highRole = mock(Role.class);
        List<Role> roles = Arrays.asList(lowRole, highRole);
        Guild guildToAdd = mock(Guild.class);
        doReturn(roles).when(guildToAdd).getRoles();
        doReturn(lowRole).when(guildToAdd).getPublicRole();
        doReturn("999").when(guildToAdd).getId();
        doReturn("test-added").when(guildToAdd).getName();
        doReturn(-1).when(lowRole).getPosition();
        doReturn(1).when(highRole).getPosition();
        doReturn("12345").when(highRole).getId();
        doReturn("999").when(lowRole).getId();

        // Add the guild
        guildDAO.addGuild(guildToAdd);

        // Check that its added
        InternalGuild returned = guildDAO.getGuildById(expected.getId());

        assertGuildEquals(expected, returned);
    }

    @Test
    public void testUpdateGuildVolume() throws SQLException {
        int vol = 60;
        guildDAO.updateGuildVolume(guilds.get(1).getId(), vol);
        InternalGuild returned = guildDAO.getGuildById(guilds.get(1).getId());
        assertEquals(returned.getVolume(), 60);
    }

    @Test
    public void testUpdateMinBaseRole() throws SQLException {
        InternalGuild expected = new InternalGuild("101", "guild-1", 100, "50", "2", "2", "1");
        guildDAO.updateMinBaseRole("101", "50");
        InternalGuild returned = guildDAO.getGuildById(expected.getId());
        assertGuildEquals(expected, returned);
    }

    @Test
    public void testUpdateMinModRole() throws SQLException {
        InternalGuild expected = new InternalGuild("101", "guild-1", 100, "1", "50", "2", "1");
        guildDAO.updateMinModRole("101", "50");
        InternalGuild returned = guildDAO.getGuildById(expected.getId());
        assertGuildEquals(expected, returned);
    }


    @Test
    public void testUpdateMinNSFWRole() throws SQLException {
        InternalGuild expected = new InternalGuild("101", "guild-1", 100, "1", "2", "50", "1");
        guildDAO.updateMinNSFWRole("101", "50");
        InternalGuild returned = guildDAO.getGuildById(expected.getId());
        assertGuildEquals(expected, returned);
    }

    @Test
    public void testUpdateMinVoiceRole() throws SQLException {
        InternalGuild expected = new InternalGuild("101", "guild-1", 100, "1", "2", "2", "50");
        guildDAO.updateMinVoiceRole("101", "50");
        InternalGuild returned = guildDAO.getGuildById(expected.getId());
        assertGuildEquals(expected, returned);
    }

}