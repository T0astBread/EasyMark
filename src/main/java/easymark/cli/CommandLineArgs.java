package easymark.cli;

public class CommandLineArgs {
    public static class Serve extends CommandLineArgs {
        public final boolean enableInsecureDebugMechanisms;

        public Serve(boolean enableInsecureDebugMechanisms) {
            this.enableInsecureDebugMechanisms = enableInsecureDebugMechanisms;
        }
    }

    public static class CreateAdmin extends CommandLineArgs {}

    public static class ResetAdminToken extends CommandLineArgs {
        public final AdminSelector adminSelector;

        public ResetAdminToken(AdminSelector adminSelector) {
            this.adminSelector = adminSelector;
        }
    }

    public static class DeleteAdmin extends CommandLineArgs {
        public final AdminSelector adminSelector;

        public DeleteAdmin(AdminSelector adminSelector) {
            this.adminSelector = adminSelector;
        }
    }

    public static class DebugSeedDatabase extends CommandLineArgs {}
}
