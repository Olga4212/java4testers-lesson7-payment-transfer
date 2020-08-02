package ru.live.toofast.payment;

import ru.live.toofast.payment.entity.Account;
import ru.live.toofast.payment.model.MoneyTransferRequest;

import java.util.Map;

public class PaymentTransferService {

    private Map<Long, Account> accounts;
    private DynamicKeyLock<Account> locks;

    public PaymentTransferService(Map<Long, Account> accounts) {
        this.accounts = accounts;
        this.locks = new DynamicKeyLock<>();
    }

    public void transfer(MoneyTransferRequest request) {

        Account fromAccount = accounts.get(request.getFrom());
        Account toAccount = accounts.get(request.getTo());

        Account first;
        Account second;

        if (fromAccount.getId() > toAccount.getId()) {
            first = fromAccount;
            second = toAccount;
        } else {
            first = toAccount;
            second = fromAccount;
        }

        locks.lock(first);
        try {
            locks.lock(second);
            try {
                Long amount = request.getAmount();
                if (fromAccount.getBalance() < amount) {
                    throw new RuntimeException("Not enough funds");
                }

                fromAccount.setBalance(fromAccount.getBalance() - amount);
                toAccount.setBalance(toAccount.getBalance() + amount);
            } finally {
                locks.unlock(second);
            }
        } finally {
            locks.unlock(first);
        }
    }
}
