package xyz.etcd.manager;

import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import xyz.etcd.manager.form.LoginForm;

public class main {
    public static void main(String[] args) {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginForm defaultForm=new LoginForm();
            }
        });
    }
}
