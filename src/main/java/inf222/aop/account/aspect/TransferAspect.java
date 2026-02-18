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

    @Around("execution(* *(..)) && @annotation(transferAnnotation)")    // intercepts any method annotated with @Transfer. execution(...) restricts it strictly to method execution.
    public Object logTransfer(ProceedingJoinPoint pjp, Transfer transferAnnotation) throws Throwable {

        Logger logger = LoggerFactory.getLogger(pjp.getTarget().getClass());
        Object[] args = pjp.getArgs();

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String[] methodParams = signature.getParameterNames();

        Object result = null;
        boolean success = false;

        try {
            result = pjp.proceed();
            success = (result instanceof Boolean) && ((Boolean) result);

            // Log international transfers (INFO)
            if (transferAnnotation.internationalTransfer()) {
                logger.info(logInternationalTransfer(args));
            }

            // Log transfers above threshold (INFO)
            double amount = (Double) args[2];
            if (amount > transferAnnotation.LogTransferAbove()) {
                logger.info(logTransferAbove(args, transferAnnotation.LogTransferAbove()));
            }

            // Log errors (ERROR) if the transfer failed
            if (!success && transferAnnotation.logErrors()) {
                logger.error(logErrors(args, methodName, methodParams));
            }

            return result;

        } catch (Throwable ex) {
            // Log errors (ERROR) on exception
            if (transferAnnotation.logErrors()) {
                logger.error(logErrors(args, methodName, methodParams));
            }
            throw ex;
        }
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