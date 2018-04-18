public class Main {

	public static void main(String[] args) throws Exception {
		Runtime.getRuntime().exec("rm -rf /home/potte/.indy_client");

		Anoncreds.demo();
		AnoncredsRevocation.demo();
		Ledger.demo();
		Crypto.demo();
	}
}
