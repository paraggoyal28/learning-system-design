public class Withdrawal extends Transaction {
	private double amount;
	private Keypad keypad;
	private CashDispenser cashDispenser;

	public Withdrawal(int accountNumber, Screen screen, BankDatabase bankDatabase,
					  Keypad keypad, CashDispenser cashDispenser) {
		super(accountNumber, screen, bankDatabase);
		this.keypad = keypad;
		this.cashDispenser = cashDispenser;
	}

	@Override
	public void execute() {
		Screen screen = getScreen();
		BankDatabase bankDatabase = getBankDatabase();

		screen.displayMessageLine("\nWithdrawal Options:");
		screen.displayMessageLine("1. $20");
		screen.displayMessageLine("2. $40");
		screen.displayMessageLine("3. $60");
		screen.displayMessageLine("4. $100");
		screen.displayMessageLine("5. $200");
		screen.displayMessageLine("6. Cancel Transaction");
		screen.displayMessageLine("Choose a withdrawal amount (1-6): ");

		int choice = keypad.getInput();

		switch(choice) {
			case 1: amount = 20; break;
			case 2: amount = 40; break;
			case 3: amount = 60; break;
			case 4: amount = 100; break;
			case 5: amount = 200; break;
			default:
				screen.displayMessageLine("\n Cancelling Transaction");
				return;
		}

		double availableBalance =  bankDatabase.getAvailableBalance(getAccountNumber());

		if (amount <= availableBalance) {
			if (cashDispenser.isSufficientCashAvailable(amount)) {
				bankDatabase.debit(getAccountNumber(), amount);
				cashDispenser.dispenseCash(amount);
				screen.displayMessageLine("Please take your cash.");
			} else {
				screen.displayMessageLine("\n Not enough cash available in the ATM. Please try a smaller amount");
			}
		} else {
			screen.displayMessageLine("\nInsufficient Funds in your account");
		}
	}
}
