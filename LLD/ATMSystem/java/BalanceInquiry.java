public class BalanceInquiry extends Transaction {

	public BalanceInquiry(int userAccountNumber,
						  Screen screen,
						  BankDatabase bankDatabase) {
		super(userAccountNumber, screen, bankDatabase);
	}

	@Override
	public void execute() {
    	Screen screen = getScreen();
		BankDatabase bankDatabase = getBankDatabase();

		double availableBalance = bankDatabase.getAvailableBalance(getAccountNumber());
		double totalBalance = bankDatabase.getTotalBalance(getAccountNumber());

		screen.displayMessageLine("\n ---- Balance Information --------");
		screen.displayMessage("---- Available Balance -------");
		screen.displayDollarAmount(availableBalance);
		screen.displayMessage("---- Total Balance -------");
		screen.displayDollarAmount(totalBalance);
		screen.displayMessageLine("------------------------------------");
	}
}
