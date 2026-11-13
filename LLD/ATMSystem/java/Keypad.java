import java.util.Scanner;

public class Keypad {
	private Scanner scanner;

	public Keypad() {
		scanner = new Scanner(System.in);
	}

	public int getInput() {
		return scanner.nextInt();
	}

	public double getDoubleInput() {
		return scanner.nextDouble();
	}
}
