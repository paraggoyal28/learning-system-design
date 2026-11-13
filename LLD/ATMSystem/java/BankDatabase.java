import java.util.HashMap;
import java.util.Map;

public class BankDatabase {
	private Map<Integer, Account> accounts;

    public BankDatabase() {
		accounts = new HashMap<>();
		// Can initiate accounts (Account Number, Pin, Available Balance, Total Balance)
		accounts.put(12345, new Account(12345, 1111, 1000.0, 1200.0));
		accounts.put(98765, new Account(98765, 2222, 500.0, 500.0));
	}

	private Account getAccount(int accountNumber) {
		return accounts.get(accountNumber);
	}

	public boolean authenticateUser(int userAccountNumber, int userPIN) {
		Account userAccount = getAccount(userAccountNumber);
		if (userAccount != null) {
			return userAccount.validatePin(userPIN);
		}
		return false;
	}

	public double getAvailableBalance(int userAccountNumber) {
		return getAccount(userAccountNumber).getAvailableBalance();
	}

	public double getTotalBalance(int userAccountNumber) {
		return getAccount(userAccountNumber).getTotalBalance();
	}

	public void credit(int userAccountNumber, double amount) {
		getAccount(userAccountNumber).credit(amount);
	}

	public void debit(int userAccountNumber, double amount) {
		getAccount(userAccountNumber).debit(amount);
	}
}
