


public class Account {
	private int accountNumber;
	private int pin;
	private double availableBalance;
	private double totalBalance;

	public Account(int accountNumber, int pin, double availableBalance, double totalBalance) {
		this.accountNumber = accountNumber;
		this.pin = pin;
		this.availableBalance = availableBalance;
		this.totalBalance = totalBalance;
	}

	public boolean validatePin(int userPin) {
		return this.pin == userPin;
	}

	public double getAvailableBalance() {
		return availableBalance;
	}

	public double getTotalBalance() {
		return totalBalance;
	}

	public void credit(double amount) {
		totalBalance += amount;
		// Available balance updates after clearing, simplified here for simulation!
		availableBalance += amount;
	}

	public void debit(double amount) {
		availableBalance -= amount;
		totalBalance -= amount;
	}

	public int getAccountNumber() {
		return accountNumber;
	}
}