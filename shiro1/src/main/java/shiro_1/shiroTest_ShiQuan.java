import com.alibaba.druid.util.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * 授权，也叫访问控制，即在应用中控制谁能访问哪些资源（如访问页面/编辑数据/页面操作等）。
 * 在授权中需了解的几个关键对象：主体（Subject）、资源（Resource）、权限（Permission）、角色（Role）。
 * <p>
 * 主体
 * <p>
 * 主体，即访问应用的用户，在Shiro中使用Subject代表该用户。用户只有授权后才允许访问相应的资源。
 * <p>
 * 资源
 * <p>
 * 在应用中用户可以访问的任何东西，比如访问JSP页面、查看/编辑某些数据、
 * 访问某个业务方法、打印文本等等都是资源。用户只要授权后才能访问。
 * <p>
 * 权限
 * <p>
 * 安全策略中的原子授权单位，通过权限我们可以表示在应用中用户有没有操作某个资源的权力。
 * 即权限表示在应用中用户能不能访问某个资源，如：
 * <p>
 * 访问用户列表页面
 * <p>
 * 查看/新增/修改/删除用户数据（即很多时候都是CRUD（增查改删）式权限控制）
 * <p>
 * 打印文档等等。。。
 * <p>
 * 如上可以看出，权限代表了用户有没有操作某个资源的权利，即反映在某个资源上的操作允不允许，
 * 不反映谁去执行这个操作。所以后续还需要把权限赋予给用户，即定义哪个用户允许在某个资源上做什么操作（权限），
 * Shiro不会去做这件事情，而是由实现人员提供。
 * <p>
 * Shiro支持粗粒度权限（如用户模块的所有权限）和细粒度权限（操作某个用户的权限，即实例级别的），后续部分介绍。
 * <p>
 * 角色
 * <p>
 * 角色代表了操作集合，可以理解为权限的集合，一般情况下我们会赋予用户角色而不是权限，
 * 即这样用户可以拥有一组权限，赋予权限时比较方便。典型的如：项目经理、技术总监、CTO、开发工程师等都是角色，
 * 不同的角色拥有一组不同的权限。
 * <p>
 * 隐式角色：即直接通过角色来验证用户有没有操作权限，如在应用中CTO、技术总监、开发工程师可以使用打印机，
 * 假设某天不允许开发工程师使用打印机，此时需要从应用中删除相应代码；再如在应用中CTO、技术总监可以查看用户、
 * 查看权限；突然有一天不允许技术总监查看用户、查看权限了，需要在相关代码中把技术总监角色从判断逻辑中删除掉；
 * 即粒度是以角色为单位进行访问控制的，粒度较粗；如果进行修改可能造成多处代码修改。
 * <p>
 * 显示角色：在程序中通过权限控制谁能访问某个资源，角色聚合一组权限集合；这样假设哪个角色不能访问某个资源，
 * 只需要从角色代表的权限集合中移除即可；无须修改多处代码；即粒度是以资源/实例为单位的；粒度较细。
 */
public class shiroTest_ShiQuan extends AuthorizingRealm {
    /**
     * 授权
     *
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
    }

    /**
     * 认证
     *
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.addRole("role1");
        authorizationInfo.addRole("role2");
        authorizationInfo.addObjectPermission(new BitPermission("+user1+10"));
        authorizationInfo.addObjectPermission(new WildcardPermission("user1:*"));
        authorizationInfo.addStringPermission("+user2+10");
        authorizationInfo.addStringPermission("user2:*");
        return (AuthenticationInfo) authorizationInfo;
    }
// 流程如下：
//
//  1、首先调用Subject.isPermitted或hasRole接口，其会委托给SecurityManager，而SecurityManager接着会委托给Authorizer；
//
//  2、Authorizer是真正的授权者，如果我们调用如isPermitted(“user:view”)，
//     其首先会通过PermissionResolver把字符串转换成相应的Permission实例；
//
//  3、在进行授权之前，其会调用相应的Realm获取Subject相应的角色/权限用于匹配传入的角色/权限；
//
//  4、Authorizer会判断Realm的角色/权限是否和传入的匹配，如果有多个Realm，会委托给ModularRealmAuthorizer进行循环判断，
//     如果匹配如isPermitted*/hasRole*会返回true，否则返回false表示授权失败。

// ModularRealmAuthorizer进行多Realm匹配流程：
//
//  1、首先检查相应的Realm是否实现了实现了Authorizer；
//
//  2、如果实现了Authorizer，那么接着调用其相应的isPermitted*/hasRole*接口进行匹配；
//
//  3、如果有一个Realm匹配那么将返回true，否则返回false。
//
// 如果Realm进行授权的话，应该继承AuthorizingRealm，其流程是：
//
//  1.1、如果调用hasRole*，则直接获取AuthorizationInfo.getRoles()与传入的角色比较即可；
//
//  1.2、首先如果调用如isPermitted(“user:view”)，首先通过PermissionResolver将权限字符串转换成相应的Permission实例，
//       默认使用WildcardPermissionResolver，即转换为通配符的WildcardPermission；
//
//  2、通过AuthorizationInfo.getObjectPermissions()得到Permission实例集合；
//     通过AuthorizationInfo. getStringPermissions()得到字符串集合并通过PermissionResolver解析为Permission实例；
//     然后获取用户的角色，并通过RolePermissionResolver解析角色对应的权限集合（默认没有实现，可以自己提供）；
//
//  3、接着调用Permission. implies(Permission p)逐个与传入的权限比较，如果有匹配的则返回true，否则false。

}

class BitPermission implements Permission {
    private String resourceIdentify;
    private int permissionBit;
    private String instanceId;

    public BitPermission(String permissionString) {
        String[] array = permissionString.split("\\+");
        if (array.length > 1) {
            resourceIdentify = array[1];
        }
        if (StringUtils.isEmpty(resourceIdentify)) {
            resourceIdentify = "*";
        }
        if (array.length > 2) {
            permissionBit = Integer.valueOf(array[2]);
        }
        if (array.length > 3) {
            instanceId = array[3];
        }
        if (StringUtils.isEmpty(instanceId)) {
            instanceId = "*";
        }
    }

    @Override
    public boolean implies(Permission p) {
        if (!( p instanceof BitPermission )) {
            return false;
        }
        BitPermission other = (BitPermission) p;
        if (!( "*".equals(this.resourceIdentify) || this.resourceIdentify.equals(other.resourceIdentify) )) {
            return false;
        }
        if (!( this.permissionBit == 0 || ( this.permissionBit & other.permissionBit ) != 0 )) {
            return false;
        }
        if (!( "*".equals(this.instanceId) || this.instanceId.equals(other.instanceId) )) {
            return false;
        }
        return true;
    }
}