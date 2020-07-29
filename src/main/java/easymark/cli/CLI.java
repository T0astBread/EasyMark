package easymark.cli;

import easymark.*;

import java.util.*;

public class CLI {

    public static CommandLineArgs parse(String[] args) throws UserFriendlyException {
        if (args.length == 0)
            return new CommandLineArgs.Serve();

        switch (args[0]) {
            case "serve":
                return new CommandLineArgs.Serve();

            case "create-admin":
                return new CommandLineArgs.CreateAdmin();

            case "reset-admin-token":
                if (args.length < 2)
                    throw new UserFriendlyException("reset-admin-token must be followed by an admin selector");
                return new CommandLineArgs.ResetAdminToken(parseAdminSelector(args[1]));

            case "delete-admin":
                if (args.length < 2)
                    throw new UserFriendlyException("delete-admin must be followed by an admin selector");
                return new CommandLineArgs.DeleteAdmin(parseAdminSelector(args[1]));

            default:
                throw new UserFriendlyException("Unrecognized subcommand \"" + args[0] + "\" (must be serve/create-admin/reset-admin-token/delete-admin)");
        }
    }

    /**
     * Admin selectors are always by-id if they're a valid UUID (the
     * internal ID type), otherwise they're by-administered-course.
     *
     * If you name a course in the format of a valid UUID, it's your
     * fault.
     */
    private static AdminSelector parseAdminSelector(String selectorStr) {
        try {
            UUID id = UUID.fromString(selectorStr);
            return new AdminSelector.ByID(id);

        } catch (IllegalArgumentException e) {
            return new AdminSelector.ByAdministeredCourse(selectorStr);
        }
    }
}
