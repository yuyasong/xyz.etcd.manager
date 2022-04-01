package xyz.etcd.manager.service;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EtcdClient {
    private String m_etcdUrl;
    private Client m_Client;

    private ByteSequence bytesOf(String val){
        return ByteSequence.from(val, StandardCharsets.UTF_8);
    }

    private Client getEtcdClient(){
        if(m_Client==null){
            m_Client= Client.builder().endpoints(m_etcdUrl).build();
        }
        return m_Client;
    }

    public EtcdClient(String url){
        m_etcdUrl =url;
    }

    public List<KeyValue> getRange(String prekey) throws Exception{
        GetOption getOption=GetOption.newBuilder().withPrefix(bytesOf(prekey)).build();
        CompletableFuture<GetResponse> response = getEtcdClient().getKVClient().get(bytesOf(prekey), getOption);
        List<KeyValue> kvs = response.get().getKvs();
        return kvs;
    }

    public KeyValue get(String key) throws Exception{
        CompletableFuture<GetResponse> response = getEtcdClient().getKVClient().get(bytesOf(key));
        GetResponse getResponse = response.get();
        if(getResponse.getCount()>0){
            KeyValue keyValue = getResponse.getKvs().get(0);
            return keyValue;
        }
        return null;
    }

    public void set(String key,String value,int ttl) throws Exception{
        if(ttl==0){
            getEtcdClient().getKVClient().put(bytesOf(key),bytesOf(value)).get().getHeader();
        }
        else if(ttl>0){
            long id = getEtcdClient().getLeaseClient().grant(ttl).get().getID();
            PutOption op = PutOption.newBuilder().withLeaseId(id).build();
            getEtcdClient().getKVClient().put(bytesOf(key),bytesOf(value),op).get().getHeader();
        }
    }

    public Watch.Watcher watch(String prefix,Watch.Listener listener) throws Exception{
        WatchOption rangeOption = WatchOption.newBuilder().withPrefix(bytesOf(prefix)).build();
        return getEtcdClient().getWatchClient().watch(bytesOf(prefix),rangeOption, listener);
    }

    public void del(String key) {
        getEtcdClient().getKVClient().delete(bytesOf(key));
    }

    public LeaseTimeToLiveResponse getLease(String key) throws Exception{
        KeyValue keyValue = get(key);
        CompletableFuture<LeaseTimeToLiveResponse> leaseTimeToLiveResponseCompletableFuture = getEtcdClient().getLeaseClient().timeToLive(keyValue.getLease(), LeaseOption.DEFAULT);
        LeaseTimeToLiveResponse leaseTimeToLiveResponse = leaseTimeToLiveResponseCompletableFuture.get();
        return leaseTimeToLiveResponse;
    }
}
