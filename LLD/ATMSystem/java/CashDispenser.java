public class CashDispenser {
	private final static int INITIAL_COUNT = 500; // $20 bills
	private int count;

	public CashDispenser() {
		this.count = INITIAL_COUNT;
	}

	public boolean isSufficientCashAvailable(double amount) {
		int billsRequired = (int) (amount/20);
		return count >= billsRequired;
	}

	public void dispenseCash(double amount) {
		int billsRequired = (int) (amount/20);
		count -= billsRequired;
		System.out.println("\n[Cash Dispenser] Please collect your $" + amount + " cash.");
	}
}
