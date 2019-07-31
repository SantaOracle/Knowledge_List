## What is hessian? ##

&nbsp;&nbsp;这里所说的hessian，指的是hessian序列化工具。

&nbsp;&nbsp;啥，你不知道啥是序列化？详细定义请google，这里简单说一下我个人的理解。

- 序列化：将对象转换成字节序列，便于持久化和传输
- 反序列化：将字节序列转化成对象

## Why hessian? ##

&nbsp;&nbsp;我们都知道，JAVA本身自带了序列化的功能，那么我们为什么还要选择其他的序列化工具呢？这里就拿hessian来举例：

- Hessian序列化的结果可以跨语言，跨平台
- Hessian序列化的速度和序列化结果的大小，优于JAVA

&nbsp;&nbsp;这里想着重说一下第二点，也就是为什么Hessian在序列化的速度和大小上优于JAVA原生序列化。

&nbsp;&nbsp;JAVA的序列化，会将对象本身的属性，对象类的信息，对象类继承的信息，统统进行完整的序列化。因此，JAVA原生序列化可以说是一个全面、完整的序列化过程，在进行反序列化的时候几乎不会有什么信息丢失（当然这不是说其他的工具就一定会有这种情况）

&nbsp;&nbsp;而Hessian序列化，则是仅根据对象的属性进行序列化，并且将他们的字段类型进行缩写。比如int i = 1，使用Hessian序列化，就会被表达成I i 1。

&nbsp;&nbsp;同时，Hessian当中还有一种类型是ref，即引用。如果某个字段是一个对象，其内容在之前已经序列化过了，那么在此字段序列化的时候，它会直接以引用形式进行序列化，避免重复工作。

## demo? ##

```
@Slf4j
public class HessianUtil {

    public static <T extends Serializable> byte[] serialize(T obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output ho = new Hessian2Output(bos);

        try {
            ho.writeObject(obj);
            ho.flushBuffer();
        } catch (IOException e) {
            log.error("Hessian serialize failed, e:", e);
        }

        return bos.toByteArray();
    }

    public static <T extends Serializable> T desrialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        Hessian2Input hi = new Hessian2Input(bis);
        Object obj = null;

        try {
            obj = hi.readObject();
        } catch (IOException e) {
            log.error("Hessian deserialize failed, e:", e);
        }

        return (T) obj;
    }

}
```

## Any Attention? ##

&nbsp;&nbsp;使用Hessian进行序列化的时候有几个需要注意的地方：

- Bean必须要实现java.io.Serializable接口：别问我为啥……
- 序列化和反序列化同样Bean的时候，要求包路径、类名称要完全一致：这个和我们所知的Jackson不同，Hessian是严格按照包路径+类来进行序列化和反序列化的
- 如果遇到需要序列化子类的情况，建议检查一下，子类是否有某个字段名和父类是一致的：因为Hessian针对对象进行序列化后存储的内容是以KV形式存储的，如果父子类有相同名称字段，子类的字段内容有可能会被父类覆盖掉


