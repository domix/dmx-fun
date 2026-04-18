module dmx.fun.spring.boot {
    requires dmx.fun;
    requires dmx.fun.spring;
    requires spring.context;
    requires spring.tx;
    requires static spring.boot.autoconfigure;
    requires static org.aspectj.weaver;
    requires static spring.webmvc;
    requires static spring.web;
    requires static jakarta.servlet;
    requires org.jspecify;

    exports dmx.fun.spring.boot;
    exports dmx.fun.spring.boot.web;
}
