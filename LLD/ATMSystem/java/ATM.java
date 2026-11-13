public class ATM {
	private boolean userAuthenticated;
	private int currentAccountNumber;
	private Screen screen;
	private Keypad keypad;
	private CashDispenser cashDispenser;
	private BankDatabase bankDatabase;

	private static final int BALANCE_INQUIRY = 1;
	private static final int WITHDRAWAL = 2;
	private static final int DEPOSIT = 3;
	private static final int EXIT = 4;

	public ATM() {
		userAuthenticated = false;
		currentAccountNumber = 0;
		screen = new Screen();
		keypad = new Keypad();
		cashDispenser = new CashDispenser();
		bankDatabase = new BankDatabase();
	}

	public void run() {
		while (true) {
			screen.displayMessageLine("\n Welcome to the ATM!");
			while (!userAuthenticated) {
				authenticateUser();
			}
			performTransaction();
			userAuthenticated = false;
			currentAccountNumber = 0;
			screen.displayMessageLine("\n Thank you! Goodbye.");
		}
	}

	private void authenticateUser() {
		screen.displayMessage("\nPlease enter your account number: ");
		int accountNumber = keypad.getInput();
		screen.displayMessage("Enter your pin: ");
		int pin = keypad.getInput();
		userAuthenticated = bankDatabase.authenticateUser(accountNumber, pin);
		if (userAuthenticated) {
			currentAccountNumber = accountNumber;
		} else {
			screen.displayMessageLine("Invalid account number or PIN. Please try again.");
		}
	}

	private void performTransaction() {
		Transaction currentTransaction = null;
		boolean userExited = false;

		while (!userExited) {
			int mainMenuSelection = displayMainMenu();

			switch (mainMenuSelection) {
				case BALANCE_INQUIRY, WITHDRAWAL, DEPOSIT -> {
					currentTransaction = createTransaction(mainMenuSelection);
					currentTransaction.execute();
				}
				case EXIT -> {
					screen.displayMessageLine("\n Exiting system...");
					userExited = true;
				}
				default -> {
					screen.displayMessageLine("\n You did not enter a VALID selection. Try again.");
				}
			}
		}
	}

	private Transaction createTransaction(int type) {

		return switch (type) {
			case BALANCE_INQUIRY -> new BalanceInquiry(currentAccountNumber, screen, bankDatabase);
			case WITHDRAWAL ->  new Withdrawal(currentAccountNumber, screen, bankDatabase, keypad, cashDispenser);
			case DEPOSIT -> new Deposit(currentAccountNumber, screen, bankDatabase, keypad);
			default -> null;
		};
	}

	private int displayMainMenu() {
		screen.displayMessageLine("\nMain Menu:");
		screen.displayMessageLine("1. View my balance");
		screen.displayMessageLine("2. Withdraw cash");
		screen.displayMessageLine("3. Deposit funds");
		screen.displayMessageLine("4. Exit\n");
		screen.displayMessage("Enter a choice: ");
		return keypad.getInput();
	}


}
