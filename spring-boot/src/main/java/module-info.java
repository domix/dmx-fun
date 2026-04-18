module dmx.fun.spring.boot {
    requires dmx.fun.spring;
    requires spring.context;
    requires spring.tx;
    requires static spring.boot.autoconfigure;
    requires static org.aspectj.weaver;
    requires org.jspecify;

    exports dmx.fun.spring.boot;
}
