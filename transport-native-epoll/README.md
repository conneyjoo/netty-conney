# Native transport for Linux

See [our wiki page](https://netty.io/wiki/native-transports.html).

Manual addition native epoll so file
cd transport-native-epoll/target/classes
zip -ur ../../target/netty-transport-native-epoll-4.1.45.conney.jar META-INF
zip -ur ../../../all/target/netty-all-4.1.45.conney.jar META-INF

jar tf target/netty-transport-native-epoll-4.1.45.conney.jar | grep "libnetty_transport_native_epoll"
jar tf all/target/netty-all-4.1.45.conney.jar | grep "libnetty_transport_native_epoll"