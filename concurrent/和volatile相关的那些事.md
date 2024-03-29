&nbsp;&nbsp;在本篇文章，你会接触到以下内容：

- DCL单例模式的设计缺陷
- JMM
- 指令重排
- volatile和synchronized
- 内存屏障
- Happens-Before

## 序言 ##

&nbsp;&nbsp;JAVA并发，是java工程师进修之路上必须去详细了解的一个重要部分。相信不少人在面试的时候，都会被问到这么一个问题：

&nbsp;&nbsp;对volatile了解吗？volatile和synchronized有什么不同？

&nbsp;&nbsp;如果你的答案是：volatile能针对一个变量实现并发安全。恭喜你，在绝大部分场景下，你的答案是正确的。

&nbsp;&nbsp;但是，volatile怎么就能保证并发安全了？它的实现机制和原理是什么？它究竟和synchronized有什么不一样？

&nbsp;&nbsp;别急，这篇文章会慢慢带你深入了解volatile。

## 从一个单例开始 ##

&nbsp;&nbsp;和volatile一样，单例模式是设计模式当中，面试者同样喜欢问的一个问题。相信不少看过单例相关帖子的同学，一定会写出DCL（Double Check Lock）的单例代码：

```
public class Singleton {

    private static Singleton instance;

    private int f1;
    private int f2;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) { 
            synchronized (Singleton.class) {
                if ( instance == null ) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

&nbsp;&nbsp;这段代码被不少帖子和应届生认为是单例的标准答案。是的，这段代码确实解决了对象重复创建的问题，但是（你们要的但是来了），这段代码会产生一个新的问题，就是“半初始化”的问题：

&nbsp;&nbsp; 假如，目前有2个线程T1, T2，同时执行上述代码。

- T1：执行到instance = new Singleton() 这一行
- T2：执行到第一个instance == null 这一行

&nbsp;&nbsp;此时，有可能发生一种情况，那就是T2在获取instance的时候，得到的是一个未被初始化的instance，此时如果用这个instance进行判空，则会触发jvm异常。

&nbsp;&nbsp;出现这种情况，是因为instance = new Singleton()这行代码，它不是原子性的。在JVM中，它可以拆分成3条原子命令：

```
memory = allocate();	//1：分配对象的内存空间
initInstance(memory);	//2：初始化对象（对f1、f2初始化）
instance = memory;		//3：设置instance指向刚分配的内存地址
```

&nbsp;&nbsp;如果是按顺序执行这3条指令，那其实在任何情况下是没什么问题的，然鹅，编译器针对代码会做一系列的优化，其中一步叫做指令重排，这一步就有可能导致上述代码出现问题。

## 指令重排 ##

&nbsp;&nbsp;指令重排在编译器层面和在机器层面各有他们的定义：

- 编译器层面：java代码经过编译后，可以对其中没有相互关联的指令进行重排，以优化执行性能
- 机器层面：CPU采用了允许将多条指令不按程序规定的顺序分开发送给各相应电路单元处理。

&nbsp;&nbsp;懵逼了吗？我也快了，那么我们结合上一节的例子来个说人话的模式。

&nbsp;&nbsp;极限假设一下，如果1~3指令，各自执行耗时如下：

- 1：10毫秒
- 2：1小时
- 3：30分钟

&nbsp;&nbsp;要知道，2操作是依赖于1的（地址都没申请下来，初始化个毛啊），所以1和2的执行顺序是能够得到CPU保证的。

&nbsp;&nbsp;但是3就不一样了，3不依赖于2，只要1操作把空间申请下来，内存地址自然就知道了。

&nbsp;&nbsp;OK，那么我们再假设指令2和指令3可以由不同的单路单元处理，那么此时编译器很有可能把123指令的执行顺序，重排成132。

&nbsp;&nbsp;这样就会导致一个结果：instance已经拥有了指向堆内存当中对象的地址，但是此时这个对象还没有初始化完毕。换句话来说，如果在这个阶段，任何针对这个instance的操作，都没有办法正确进行。

&nbsp;&nbsp;好，让我们从编译器层面回到我们的代码层面。此时T1已经申请了内存空间，并且把对象地址引用赋值给了instance，但是对象并没有完成初始化。T2在这个时候用这个instance判了一下空，于是乎，蹦沙卡拉卡……

## volatile它lei了 ##

&nbsp;&nbsp;嗯，上面这种情况你们已经知道我想说的其中一种解决方式了。那就是在声明instance变量时，将其声明为volatile变量：

```
private static volatile Singleton instance;
```

&nbsp;&nbsp;当一个变量声明为volatile之后，它就具备了两种特性：

- 保证变量对所有线程的可见性
- 禁止指令重排序优化

&nbsp;&nbsp;我们先把知识点列完，其中，volatile禁止指令重排序优化的方式，是在读写volatile变量的指令前后，增加内存屏障。

&nbsp;&nbsp;亿脸懵逼不？是的，我第一次见到这么多概念的时候，我也很懵逼。没关系，我们一点点来。想要理解以上两段话的含义，我们需要解决3个问题：

- 什么是可见性？
- 什么是内存屏障？
- 内存屏障的作用是什么？

&nbsp;&nbsp;插一句废话，如果你是在面试，面试官问你怎么禁止重排，你可以回答使用内存屏障；如果面试官问你，内存屏障的作用是什么，你可以回答，禁止重排序……

### 什么是可见性： ###

&nbsp;&nbsp;可见性是java内存模型设计当中处理的主要问题之一，剩下两个是原子性和有序性。

- 原子性：如果要执行的命令，在其执行期间，不会受到其他指令干扰，而完整执行，并一定能获取到预期结果。我们称，这些命令具有原子性。（原子性不一定针对单条CPU指令，做好同步措施的一系列指令，也是具有原子性的）
- 可见性：一个线程修改了共享变量的值，其他线程能够立即得知这个修改（注意，得知修改的意思，是别的线程如果要读/写该共享变量，必须重新从主内存当中获取这个变量的最新值，由此“得知”这个变量被修改）
- 有序性：如果在本线程观察，所有操作都是有序的；如果在一个线程当中观察另一个线程，所有操作都是无序的。（前半句指“线程内变现为串行语义”，后半句指“指令重排序”现象和“线程工作内存和主内存同步延迟”现象）

&nbsp;&nbsp;如果你读了上述定义，对可见性还是没什么感觉，那么说明你对JAVA内存模型还不是很了解。

&nbsp;&nbsp;在这里，我们可以把JAVA内存，粗暴拆分成以下几个部分（以下定义，看看就好，非官网定义稍有差别）

- 主内存工作区
    - 主内存对象地址集合
- 线程工作区
    - 线程工作内存
- 执行引擎（CPU）

&nbsp;&nbsp;定义好了各部分组件，我们再定义以下操作。JAVA内存模型当中，一共有以下几种操作，他们都是原子性操作：

- 1.read —— 主内存对象地址集合 -> 线程工作区。把主内存当中的对象内容传入到工作线程当中。
- 2.load —— 线程工作区 -> 线程工作内存。把刚刚放到线程工作区的对象内容，copy一份副本，放到线程自己的副本队列当中。
- 3.use —— 线程工作内存 -> 执行引擎。使用执行引擎，对线程内的副本对象进行操作。
- 4.assign —— 执行引擎 -> 线程工作内存。将执行引擎更改完之后的对象内容，赋值到线程工作内存的副本队列当中。
- 5.store —— 线程工作内存 -> 主内存工作区。将更新后的对象副本，传到主内存。
- 6.write —— 主内存工作区 -> 主内存对象地址集合。将线程传入的新对象内容，写入到主内存的对象集合当中，覆盖原有的对象内容。

&nbsp;&nbsp;从上，我们可以看到，当我们针对主内存当中的一个对象进行修改的时候，它的操作并不是原子性的。也就是说，不同线程之间，从主内存当中拷贝的对象副本有可能是不一样的（比如有一些拷贝到线程内存之后，对其多次修改，再刷入主存，期间有其他线程从主内存当中获取了未修改时的对象内容）

&nbsp;&nbsp;volatile干的事，就是让CPU修改完后的对象内容，立即刷新/同步到主内存。其他线程想要访问这个对象时，无法使用工作线程中的变量副本，必须从主内存当中重新拷贝，以此保证该对象的线程间可见性。

&nbsp;&nbsp;而成就这么骚操作的一个工具，就是volatile使用的，内存屏障。

### 什么是内存屏障&作用： ###

&nbsp;&nbsp;通过阅读上文，我们知道了编译器有指令重排这么骚的一个操作，也就有可能把A线程的最终写入主存操作重排到B线程的读取操作之前。内存屏障就是一个禁止指令重排的一堵墙，换句话来说，就是指令重排无法逾越内存屏障。

&nbsp;&nbsp;用之前的例子，那就是volatile的变量，在执行引擎算出新的值之后，其执行的下一条指令是：

```
lock addl $0x0,(%esp)
```

&nbsp;&nbsp;其含义，是告诉编译器，在进行指令重排时，禁止将后边的指令重排到该屏障之前的位置。同时，它还触发了一次，针对volatile变量的store和write操作，保证修改之后新的对象的值会第一时间同步到主内存。

&nbsp;&nbsp;简单来说，两句话：

- 新的值会立即同步到主内存
- 其他线程在访问该变量时，都会重新从主内存获取变量值

&nbsp;&nbsp;由此，该变量的线程间可见性，足以得到保证。再回到之前的单例，保证了线程间可见性，自然不会让其他线程获取到“未初始化完成”的变量。

## synchronized疑点： ##

&nbsp;&nbsp;再回到刚刚的代码，我们再加上一行打印的语句：

```
public class Singleton {

    private static Singleton instance;

    private int f1;
    private int f2;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) { 
            synchronized (Singleton.class) {
                if ( instance == null ) {
                    instance = new Singleton();
                    System.out.println(instance);
                }
            }
        }
        return instance;
    }
}
```

&nbsp;&nbsp;我们说到，代码有指令重排，那么岂不是有可能synchronized块中的语句也会被重排？那访问到打印那一句的时候，访问的也是未初始化完成的对象？

&nbsp;&nbsp;首先，告诉你，是的。其次，再告诉你，别担心，不会发生你料想中的情况。原因是，同步块中的代码，属于单线程执行，而针对单线程执行的代码，编译器即时有指令重排，也会遵循先行发生原则（Happens-Before）。

## 先行发生原则： ##

&nbsp;&nbsp;先行发生原则，也就是我们常说的，Happens-Before原则。

&nbsp;&nbsp;国际惯例，在了解这个原则之前，我们要解决几个方向性问题：

- 为什么会有这个原则？
- 这个原则能解决什么问题？
- 这个原则内容有什么？

&nbsp;&nbsp;前两个问题，可以统一回答。

&nbsp;&nbsp;首先，通过本文之前的介绍，我们了解了，编译器在帮我们编译JAVA代码的时候，会出于“优化”的目的，对我们的代码进行重排序。

&nbsp;&nbsp;但是呢，你重排序没问题，你不能XJB排吧？于是这些先行发生原则，就是针对编译器重排做出的一些限制，这是一方面。另一方面则是为了解决线程之间可见性的问题，设计了这些原则。

&nbsp;&nbsp;接下来是这些原则的具体内容。如果仅仅是关注volatile的同学可以直接略过，看下一节内容：

### 规则一：程序的顺序性规则 ###

> 一个线程中，按照程序的顺序，前面的操作happens-before后续的任何操作。

&nbsp;&nbsp;这里的顺序性，指的是，在单线程环境下，我们可以按照代码编写的顺序推演执行结果。代码经过编译之后，也许它的执行顺序和我们预想的并不是严格一致，但是其执行执行结果一定和我们预期相符。

### 规则二：volatile规则 ###

> 对一个volatile变量的写操作，happens-before后续对这个变量的读操作。

&nbsp;&nbsp;这个原则，确保了volatile变量的线程间可见性。即，一个线程对volatile变量修改之后，会立即同步主存。其他线程想要使用这个变量，必须从主存获取最新值再进行后续操作。

### 规则三：传递性规则 ###

> 如果A happens-before B，B happens-before C，那么A happens-before C。

&nbsp;&nbsp;这个，很好理解，毕竟初中高中的一些数学定理也有这种传递性的例子。

### 规则四：管程中的锁规则 ###

> 针对同一个锁，unlock happens-before后续的lock

&nbsp;&nbsp;这个也很好理解，不解锁，怎么释放共享资源所有权，后续的线程怎么获取所有权。

### 规则五：线程的start()规则 ###

> 主线程A启动线程B，线程B中可以看到主线程启动B之前的操作。也就是start() happens before 线程B中的操作。

&nbsp;&nbsp;来个简单版的：Thread.start() happens-before这条线程其他任意操作

&nbsp;&nbsp;这个规则要是有什么疑问的话，建议重修一下计算机专业，操作系统课程，线程相关内容

### 规则六：线程的join()规则 ###

> 主线程A等待子线程B完成，当子线程B执行完毕后，主线程A可以看到线程B的所有操作。也就是说，子线程B中的任意操作，happens-before join()的返回。

&nbsp;&nbsp;简单版：

- 两条线程A，B
- A创建的B（A是主线程）
- A调用B.start()
- A调用A.join()，A被阻塞
- B线程生命周期结束，唤醒A
- A继续执行后续逻辑

&nbsp;&nbsp;[线程join相关的知识](https://www.jianshu.com/p/fc51be7e5bc0)

## volatile并不是万能的： ##

&nbsp;&nbsp;我们之前说，volatile声明的变量，具有线程间可见性和禁止指令重排。但是很多人会误理解成，volatile声明的变量是一个原子变量，这是不对的。

&nbsp;&nbsp;volatile声明的变量，仅仅是其读、写具有了“原子性”，但是对于变量的操作，并不保证其原子性。换句话来说，针对于变量的操作，读、写只是操作的一部分，volatile不能涵盖除这两个操作之外，其他操作的原子性。比如：

- 基本类型的自增（如count++）
- 对象的任何非原子成员调用（包括成员变量和方法）

&nbsp;&nbsp;如果希望上述操作也具有原子性，请使用synchronized或者lock等措施进行保证。

&nbsp;&nbsp;这里简单说一下count++为什么使用了volatile之后，依然无法保证线程安全。

&nbsp;&nbsp;针对count++这一条语句，我们使用jclasslib反编译查看一下它的字节码，可以看到，它的指令不止一个：

```
0 getstatic #7 <org/tech/accumulation/demo/pattern/design/Singleton.count>
3 iconst_1
4 iadd
5 putstatic #7 <org/tech/accumulation/demo/pattern/design/Singleton.count>
8 return
```

&nbsp;&nbsp;在使用了volatile关键字后，getstatic操作把count的内容从主存同步到操作栈顶，这是原子操作，没有问题。但是在执行iconst_1和iadd的时候，其他线程可能也从主存同步了未变更的count值，或者有新的值已经刷新到主存当中。这时候，该线程执行putstatic，可能会把较小的一个值同步到主存当中。