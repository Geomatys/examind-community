
/**
 * Provide a single examind instance used by the webui.
 * 
 * @author Johann Sorel (Geomatys)
 */
var module = angular.module('examind-instance', ['examind-factory','webui-config']);


module.factory('Examind', function($cookieStore, CstlConfig, ExamindFactory) {
        return ExamindFactory.create($cookieStore.get(CstlConfig['cookie.cstl.url']));
    });
    
    
module.factory('Permission', function(Examind) {

        var self = {};

        var _account = null;

        self.getAccount = function() {
            return _account;
        };

        self.setAccount = function(acc) {
            _account = acc;
        };

        self.hasRole = function(role) {
            if(_account && _account.roles) {
                return _account.roles.indexOf(role) !== -1;
            }
            return false;
        };

        self.hasPermission = function(perm) {
            if(self.hasRole('cstl-admin')){
                return true;
            }else if(self.hasRole('cstl-publish')){
                return perm === "publish" ||  perm === "data" || perm === "contribute" ||  perm === "moderate";
            } else if(self.hasRole('cstl-data')){
                return perm === "data" || perm === "contribute";
            }
            return false;
        };

        self.promise = Examind.authentication.account().then(
                function(response){
                    _account=response.data;
                }
            );
        return self;
    });