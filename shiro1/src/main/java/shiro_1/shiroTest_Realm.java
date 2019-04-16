import org.apache.shiro.authc.*;
import org.apache.shiro.realm.Realm;

/**
 * ini配置文件指定自定义Realm实现(shiro-realm.ini)
 * 以后一般继承AuthorizingRealm（授权）即可；其继承了AuthenticatingRealm（即身份验证），而且也间接继承了CachingRealm（带有缓存实现）。其中主要默认实现如下：
 *
 * org.apache.shiro.realm.text.IniRealm：[users]部分指定用户名/密码及其角色；[roles]部分指定角色即权限信息；
 *
 * org.apache.shiro.realm.text.PropertiesRealm： user.username=password,role1,role2指定用户名/密码及其角色；
 * role.role1=permission1,permission2指定角色及权限信息；
 *
 * org.apache.shiro.realm.jdbc.JdbcRealm：通过sql查询相应的信息，
 * 如“select password from users where username = ?”获取用户密码，
 * “select password, password_salt from users where username = ?”获取用户密码及盐；
 * “select role_name from user_roles where username = ?”获取用户角色；
 * “select permission from roles_permissions where role_name = ?”获取角色对应的权限信息；也可以调用相应的api进行自定义sql；
 */
public class shiroTest_Realm implements Realm {
    /**
     * @return 返回一个唯一的Realm名字
     */
    @Override
    public String getName() {
        return "shiroTest_Realm";
    }

    /**
     *
     * @param authenticationToken token
     * @return 判断此Realm是否支持此Token
     */
    @Override
    public boolean supports(AuthenticationToken authenticationToken) {
        return authenticationToken instanceof UsernamePasswordToken;
    }

    /**
     *   根据Token获取认证信息
     * @param authenticationToken token
     * @return 认证信息
     * @throws AuthenticationException 认证异常
     */
    @Override
    public AuthenticationInfo getAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        System.out.println("进入自定义realm方法----->身份认证");
        //用户名
        String username = (String) authenticationToken.getPrincipal();
        //密码
        String password = new String((char[]) authenticationToken.getCredentials());
        if(!"zheng".equals(username)) {
            throw new UnknownAccountException(); //如果用户名错误
        }
        if(!"1992".equals(password)) {
            throw new IncorrectCredentialsException(); //如果密码错误
        }

        AuthenticationInfo info = new SimpleAuthenticationInfo(username,password,getName());
        return info;
    }

    /**
     * Realm：域，Shiro从从Realm获取安全数据（如用户、角色、权限），
     * 就是说SecurityManager要验证用户身份，那么它需要从Realm获取相应的用户进行比较以确定用户身份是否合法；
     * 也需要从Realm得到用户相应的角色/权限进行验证用户是否能进行操作；
     * 可以把Realm看成DataSource，即安全数据源。
     * 如我们之前的ini配置方式将使用org.apache.shiro.realm.text.IniRealm。
     */

    /**
     * 多Realm配置
     * #声明一个realm
     * myRealm1=com.github.zhangkaitao.shiro.chapter2.realm.MyRealm1
     * myRealm2=com.github.zhangkaitao.shiro.chapter2.realm.MyRealm2
     * #指定securityManager的realms实现
     * securityManager.realms=$myRealm1,$myRealm2
     *
     * securityManager会按照realms指定的顺序进行身份认证。此处我们使用显示指定顺序的方式指定了Realm的顺序，
     * 如果删除“securityManager.realms=$myRealm1,$myRealm2”，
     * 那么securityManager会按照realm声明的顺序进行使用（即无需设置realms属性，其会自动发现），
     * 当我们显示指定realm后，其他没有指定realm将被忽略，如“securityManager.realms=$myRealm1”，
     * 那么myRealm2不会被自动设置进去。
     */

    /**
     * Authenticator及AuthenticationStrategy
     * Authenticator的职责是验证用户帐号，是Shiro API中身份验证核心的入口点：
     * 如果验证成功，将返回AuthenticationInfo验证信息；此信息中包含了身份及凭证；如果验证失败将抛出相应的AuthenticationException实现。
     *
     * SecurityManager接口继承了Authenticator，另外还有一个ModularRealmAuthenticator实现，
     * 其委托给多个Realm进行验证，验证规则通过AuthenticationStrategy接口指定，默认提供的实现：
     *
     * FirstSuccessfulStrategy：只要有一个Realm验证成功即可，只返回第一个Realm身份验证成功的认证信息，其他的忽略；
     *
     * AtLeastOneSuccessfulStrategy：只要有一个Realm验证成功即可，
     * 和FirstSuccessfulStrategy不同，返回所有Realm身份验证成功的认证信息；
     *
     * AllSuccessfulStrategy：所有Realm验证成功才算成功，
     * 且返回所有Realm身份验证成功的认证信息，如果有一个失败就失败了。
     *
     * ModularRealmAuthenticator默认使用AtLeastOneSuccessfulStrategy策略。
     *
     * 假设我们有三个realm：
     *
     * myRealm1： 用户名/密码为zhang/123时成功，且返回身份/凭据为zhang/123；
     *
     * myRealm2： 用户名/密码为wang/123时成功，且返回身份/凭据为wang/123；
     *
     * myRealm3： 用户名/密码为zhang/123时成功，且返回身份/凭据为zhang@163.com/123，
     *            和myRealm1不同的是返回时的身份变了；
     */
}
