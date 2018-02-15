import sun.management.Agent;

public class Main {

	public static void main(String[] args) throws Exception {
		Runtime.getRuntime().exec("rm -rf /home/potte/.indy_client");
		GettingStarted.demo();
//		Ledger.demo();
//		Anoncreds.demo();
	}
}
