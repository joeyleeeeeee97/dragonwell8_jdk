/*
 * @test
 * @summary Verify wisp internal logic in a WispControlGroup container can be preempted
 * @library /lib/testlibrary
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Xcomp -Dcom.alibaba.wisp.carrierEngines=4 WispControlGroupPreemptTest
 */

import com.alibaba.wisp.engine.WispEngine;
import sun.misc.SharedSecrets;

import java.security.MessageDigest;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;

import static jdk.testlibrary.Asserts.*;

public class WispControlGroupPreemptTest {

    private static final int timeOut = 2500;

    private static FutureTask getFutureTask(Runnable runnable) {
        return new FutureTask<>(() -> {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < timeOut) {
                runnable.run();
            }
            Class<?> WispTaskClazz = Class.forName("com.alibaba.wisp.engine.WispTask");
            Field field = WispTaskClazz.getDeclaredField("preemptCount");
            field.setAccessible(true);
            int preemptCount = field.getInt(SharedSecrets.getWispEngineAccess().getCurrentTask());
            assertTrue(preemptCount > 0, "preempt doesn't happen in the wispTask container!!!");
            return null;
        });
    }

    public static void main(String[] args) throws Exception {

        FutureTask future = getFutureTask(() -> {
            try {
                loop(500000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            Class<?> WispControlGroupClazz = Class.forName("com.alibaba.wisp.engine.WispControlGroup");
            Method createMethod = WispControlGroupClazz.getDeclaredMethod("create", int.class);
            createMethod.setAccessible(true);
            ExecutorService cg = (ExecutorService) createMethod.invoke(null, 50);
            cg.submit(future);
            future.get();
        } catch (Exception e) {
            assertTrue(false, e.toString());
        }
    }

    private static void loop(int count) throws Exception {
        while (count > 0) {
            count--;
        }
    }
}
