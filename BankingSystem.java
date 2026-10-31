import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Banking Management System
 * OOP concepts: classes/objects, constructors, encapsulation, inheritance,
 * polymorphism, abstraction, interface implementation, method overriding,
 * access modifiers.
 */

// ---------- Interface ----------
interface Transactional {
    void deposit(double amount);
    void withdraw(double amount) throws InsufficientFundsException;
    double getBalance();
}

// ---------- Custom exceptions ----------
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String accountNumber) {
        super("No account found with number " + accountNumber);
    }
}

// ---------- Transaction record ----------
class Transaction {
    enum Type { DEPOSIT, WITHDRAWAL }

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Type type;
    private final double amount;
    private final double balanceAfter;
    private final LocalDateTime time;

    Transaction(Type type, double amount, double balanceAfter) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.time = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("%s | %-10s | %10.2f | Balance: %10.2f",
                time.format(FORMAT), type, amount, balanceAfter);
    }
}

// ---------- Abstract base class ----------
abstract class Account implements Transactional {
    private static int nextNumber = 1001;               // shared by all accounts

    private final String accountNumber;                 // encapsulated (private)
    private final String holderName;
    private double balance;
    private final List<Transaction> history = new ArrayList<>();

    protected Account(String holderName, double openingBalance) {
        if (holderName == null || holderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (openingBalance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        this.accountNumber = "ACC" + nextNumber++;
        this.holderName = holderName.trim();
        this.balance = openingBalance;
    }

    // Abstract methods: each subclass must provide its own version
    public abstract String getAccountType();
    protected abstract double getMinimumBalance();      // protected: subclasses only

    @Override
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        balance += amount;
        history.add(new Transaction(Transaction.Type.DEPOSIT, amount, balance));
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        double available = balance - getMinimumBalance();
        if (amount > available) {
            throw new InsufficientFundsException(String.format(
                    "Insufficient funds. You can withdraw up to %.2f.", Math.max(available, 0)));
        }
        balance -= amount;
        history.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, balance));
    }

    @Override
    public double getBalance() { return balance; }

    public String getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }

    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);   // callers cannot modify it
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | Balance: %.2f",
                accountNumber, getAccountType(), holderName, balance);
    }
}

// ---------- Subclasses (inheritance + overriding) ----------
class SavingsAccount extends Account {
    private static final double MIN_BALANCE = 500.0;

    public SavingsAccount(String holderName, double openingBalance) {
        super(holderName, openingBalance);
        if (openingBalance < MIN_BALANCE) {
            throw new IllegalArgumentException(
                    "Savings accounts need an opening balance of at least " + MIN_BALANCE);
        }
    }

    @Override
    public String getAccountType() { return "Savings"; }

    @Override
    protected double getMinimumBalance() { return MIN_BALANCE; }
}

class CurrentAccount extends Account {
    private static final double OVERDRAFT_LIMIT = 5000.0;

    public CurrentAccount(String holderName, double openingBalance) {
        super(holderName, openingBalance);
    }

    @Override
    public String getAccountType() { return "Current"; }

    @Override
    protected double getMinimumBalance() { return -OVERDRAFT_LIMIT; }
}

// ---------- Bank (manages accounts) ----------
class Bank {
    private final Map<String, Account> accounts = new HashMap<>();

    public Account openAccount(String type, String name, double openingBalance) {
        Account account;
        switch (type) {
            case "1":
                account = new SavingsAccount(name, openingBalance);
                break;
            case "2":
                account = new CurrentAccount(name, openingBalance);
                break;
            default:
                throw new IllegalArgumentException("Unknown account type.");
        }
        accounts.put(account.getAccountNumber(), account);
        return account;
    }

    public Account find(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber.trim().toUpperCase());
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }
}

// ---------- Main program ----------
public class BankingSystem {

    private static String readText(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }

    private static double readAmount(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    public static void main(String[] args) {
        Bank bank = new Bank();
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== Banking Management System ===");
            System.out.println("1. Open account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Check balance");
            System.out.println("5. Transaction history");
            System.out.println("6. Exit");
            String choice = readText(sc, "Choose an option: ").trim();

            try {
                switch (choice) {
                    case "1": {
                        String type = readText(sc, "Account type (1 = Savings, 2 = Current): ").trim();
                        String name = readText(sc, "Holder name: ");
                        double opening = readAmount(sc, "Opening balance: ");
                        Account acc = bank.openAccount(type, name, opening);
                        System.out.println("Account created: " + acc);
                        break;
                    }
                    case "2": {
                        Account acc = bank.find(readText(sc, "Account number: "));
                        acc.deposit(readAmount(sc, "Amount to deposit: "));
                        System.out.printf("Deposit successful. New balance: %.2f%n", acc.getBalance());
                        break;
                    }
                    case "3": {
                        Account acc = bank.find(readText(sc, "Account number: "));
                        acc.withdraw(readAmount(sc, "Amount to withdraw: "));
                        System.out.printf("Withdrawal successful. New balance: %.2f%n", acc.getBalance());
                        break;
                    }
                    case "4": {
                        Account acc = bank.find(readText(sc, "Account number: "));
                        System.out.println(acc);
                        break;
                    }
                    case "5": {
                        Account acc = bank.find(readText(sc, "Account number: "));
                        if (acc.getHistory().isEmpty()) {
                            System.out.println("No transactions yet.");
                        } else {
                            for (Transaction t : acc.getHistory()) {
                                System.out.println(t);
                            }
                        }
                        break;
                    }
                    case "6":
                        System.out.println("Thank you for banking with us. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option, try 1-6.");
                }
            } catch (InsufficientFundsException | AccountNotFoundException
                     | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        sc.close();
    }
}
