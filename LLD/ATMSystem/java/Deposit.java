public class Deposit extends Transaction {
	private double amount;
	private Keypad keypad;

	public Deposit(int userAccountNumber, Screen screen, BankDatabase bankDatabase,
				   Keypad keypad) {
		super(userAccountNumber, screen, bankDatabase);
		this.keypad = keypad;
	}

	@Override
	public void execute() {
		Screen screen = getScreen();
		BankDatabase bankDatabase = getBankDatabase();

		screen.displayMessage("Enter a deposit amount in CENTS (eg. 500 for $5.00): ");
		int cents = keypad.getInput();
		amount = cents/100.0;

		screen.displayMessageLine("\n Please insert a deposit envelope containing: " + amount);
		bankDatabase.credit(getAccountNumber(), amount);
		screen.displayMessageLine("\n Your deposit has been received. Note: The total balance will reflect once verified.");
	}
}
