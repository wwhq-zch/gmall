package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
    private static final String pubKeyPath = "D:\\test\\project\\rsa.pub";
    private static final String priKeyPath = "D:\\test\\project\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ8.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDI2NzcxNTR9.kE_lUtj-KOrWgSaO_fMVofGRcnIK72rKUEMBmW43vxvvvczOoGvR4uJgGhPz2Lgmpt5LrxojfGfoNWb2BLZAYEljp9ECoKobBVdaSQxQWmBJbMpqg27dkLggrQdty9ZmayLc3vrUzuUNkEs7Uw7WeH4hEAhfqnzvtTaPsbbLQWdVCLtCa8cbUoqWWhHyNnbG9c_neuagIWGPh10TWuWCY5pLBt3rPIR4MQxD9S9RnuK--lgVlhswCrrV8SG9x6exRVI0BHnRhicYSKqb1BxjDa_khFq9KdDU0JvmijApiHYK9U7R2p6AdPAIN8P1XxR1v22znvz6OjzEJFt9VTNB_g";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }

}
