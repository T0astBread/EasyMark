package easymark;

import easymark.cli.*;
import org.junit.*;

import java.util.*;

public class CommandLineParserTest {
    @Test
    public void testDefaultSubcommand() throws UserFriendlyException {
        String[] input = new String[] {};
        CommandLineArgs output = CLI.parse(input);
        Assert.assertEquals(CommandLineArgs.Serve.class, output.getClass());
    }

    @Test
    public void testServe() throws UserFriendlyException {
        String[] input = new String[] { "serve" };
        CommandLineArgs output = CLI.parse(input);
        Assert.assertEquals(CommandLineArgs.Serve.class, output.getClass());
    }

    @Test
    public void testCreateAdmin() throws UserFriendlyException {
        String[] input = new String[] { "create-admin" };
        CommandLineArgs output = CLI.parse(input);
        Assert.assertEquals(CommandLineArgs.CreateAdmin.class, output.getClass());
    }

    @Test
    public void testResetAdminTokenByID() throws UserFriendlyException {
        final String TEST_UUID = "d805ec16-2b76-4cb7-8ae6-e61b307014fc";

        String[] input = new String[] { "reset-admin-token", TEST_UUID };
        CommandLineArgs output = CLI.parse(input);

        Assert.assertEquals(CommandLineArgs.ResetAdminToken.class, output.getClass());
        Assert.assertEquals(AdminSelector.ByID.class,
                ((CommandLineArgs.ResetAdminToken) output).adminSelector.getClass());
        Assert.assertEquals(UUID.fromString(TEST_UUID),
                ((AdminSelector.ByID) ((CommandLineArgs.ResetAdminToken) output).adminSelector).id);
    }

    @Test
    public void testDeleteAdminByCourse() throws UserFriendlyException {
        final String TEST_COURSE = "Test-Course 1234";

        String[] input = new String[] { "delete-admin", TEST_COURSE };
        CommandLineArgs output = CLI.parse(input);

        Assert.assertEquals(CommandLineArgs.DeleteAdmin.class, output.getClass());
        Assert.assertEquals(AdminSelector.ByAdministeredCourse.class,
                ((CommandLineArgs.DeleteAdmin) output).adminSelector.getClass());
        Assert.assertEquals(TEST_COURSE,
                ((AdminSelector.ByAdministeredCourse) ((CommandLineArgs.DeleteAdmin) output).adminSelector).courseName);
    }
}
