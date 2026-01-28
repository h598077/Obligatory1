package inf222.aop.account.aspect;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import inf222.aop.account.Account;
import inf222.aop.account.annotation.Transfer;

@Aspect
public class TransferAspect {

    @Around("@annotation(transferAnnotation)")    // intercepts any method annotated with @Transfer.
    public Object logTransfer(ProceedingJoinPoint pjp, Transfer transferAnnotation) throws Throwable {

        Object result = pjp.proceed();  //Call the Original Method

        Logger logger = LoggerFactory.getLogger(pjp.getTarget().getClass());
        Level level = transferAnnotation.value(); // Logging level
        Object[] args = pjp.getArgs();   // Method arguments

        if (transferAnnotation.internationalTransfer()) {  //Log international transfers
            String msg = logInternationalTransfer(args);
            logger.atLevel(level).log(msg);
        }

        Double amount = (Double) args[2];
        if (amount > transferAnnotation.LogTransferAbove()) {   // Log transfers above a certain amount
            String msg = logTransferAbove(args, transferAnnotation.LogTransferAbove());
            logger.atLevel(level).log(msg);
        }

        if (transferAnnotation.logErrors() && Boolean.FALSE.equals(result)) {  // Log Errors
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            String methodName = signature.getName();
            String[] paramNames = signature.getParameterNames();

            String msg = logErrors(args, methodName, paramNames);
            logger.atLevel(level).log(msg);
        }

        return result;
    }
    // Helper Methods
    private String logInternationalTransfer(Object[] methodArgs) {
        Account from = (Account) methodArgs[0];
        Account to = (Account) methodArgs[1];
        Double amount = (Double) methodArgs[2];

        var message = String.format("International transfer from %s to %s, %s %s converted to %s",
                from.getAccountName(),
                to.getAccountName(),
                amount,
                from.getCurrency(),
                to.getCurrency());
        return message;
    }

    private String logTransferAbove(Object[] methodArgs, double value) {
        Account from = (Account) methodArgs[0];
        Account to = (Account) methodArgs[1];
        Double amount = (Double) methodArgs[2];

        var message = String.format("Transfer above %s from %s to %s, amount: %s",
                value,
                from.getAccountName(),
                to.getAccountName(),
                amount);
        return message;
    }

    private String logErrors(Object[] methodArgs, String methodName, String[] methodParams) {
        Account from = (Account) methodArgs[0];
        Account to = (Account) methodArgs[1];
        Double amount = (Double) methodArgs[2];

        String paramsJoined = String.join(", ", methodParams);

        var message = String.format("Error in transfer from %s to %s, amount: %s %s, method: %s(%s)",
                from.getAccountName(),
                to.getAccountName(),
                amount,
                from.getCurrency(),
                methodName,
                paramsJoined);
        return message;
    }
}