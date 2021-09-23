# semaphore.acquire

java.util.concurrent.Semaphore#acquire()

java.util.concurrent.locks.AbstractQueuedSynchronizer#acquireSharedInterruptibly

```java
 public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            // 不够分配时
            doAcquireSharedInterruptibly(arg);
    }
```



-java.util.concurrent.Semaphore.NonfairSync#tryAcquireShared （非公平同步器实现）

```java
        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
```



--java.util.concurrent.Semaphore.Sync#nonfairTryAcquireShared

```java
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                // 不够分配时，返回负数,或者够分配且分配成功，返回正数
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
```



---java.util.concurrent.locks.AbstractQueuedSynchronizer#compareAndSetState

----sun.misc.Unsafe#compareAndSwapInt

----hotspot-37240c1019fd\src\share\vm\prims\unsafe.cpp

```cpp
UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSwapInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x))
  UnsafeWrapper("Unsafe_CompareAndSwapInt");
  oop p = JNIHandles::resolve(obj);
  jint* addr = (jint *) index_oop_from_field_offset_long(p, offset);
  return (jint)(Atomic::cmpxchg(x, addr, e)) == e;
UNSAFE_END
```

-----hotspot-37240c1019fd\src\share\vm\runtime\atomic.cpp

```hpp

unsigned Atomic::cmpxchg(unsigned int exchange_value,
                         volatile unsigned int* dest, unsigned int compare_value) {
  assert(sizeof(unsigned int) == sizeof(jint), "more work to do");
  return (unsigned int)Atomic::cmpxchg((jint)exchange_value, (volatile jint*)dest,
                                       (jint)compare_value);
```



-----hotspot-37240c1019fd\src\os_cpu\windows_x86\vm\atomic_windows_x86.inline.hpp

```cpp
inline jint     Atomic::cmpxchg    (jint     exchange_value, volatile jint*     dest, jint     compare_value) {
  // alternative for InterlockedCompareExchange
  int mp = os::is_MP();
  __asm {
    // 将state变量的地址存到edx
    mov edx, dest
    //  将update值存到ecx
    mov ecx, exchange_value
    // 将excepted放到eax
    mov eax, compare_value
    //判断如果是多核执行指令前就加锁，锁住总线
    LOCK_IF_MP(mp)
    //如果dest地址所在的值与寄存器eax的值相等，就将ecx寄存器中的值写到dest地址，如果不相等，将目的操作数（dest地址所在的值）赋值到eax寄存中。
    cmpxchg dword ptr [edx], ecx
  }
}
```



dword ptr[edx] 为获取地址为edx所在的int值，即dest地址所在的值，**由于这个变量地址是volatile的，所以执行这条指令必须从主存中加载而不是从CPU高速缓冲区获得。这个也可以解释state变量为什么需要是volatile的，**

#### 问题一：如果state不是volatile类型有什么问题？

**问题二： 为什么多核时需要在步骤4的时候加锁？**

**问题三： 单核为什么不需要加锁**



-java.util.concurrent.locks.AbstractQueuedSynchronizer#doAcquireSharedInterruptibly

```java
 /**
     * Acquires in shared interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```



--java.util.concurrent.locks.AbstractQueuedSynchronizer#addWaiter

```
/**
 * Creates and enqueues node for current thread and given mode.
 *
 * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
 * @return the new node
 */
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    // Try the fast path of enq; backup to full enq on failure
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    enq(node);
    return node;
}
```



- java.util.concurrent.Semaphore.FairSync#tryAcquireShared（公平同步器实现）

