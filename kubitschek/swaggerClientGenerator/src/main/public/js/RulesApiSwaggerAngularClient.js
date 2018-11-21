/*jshint -W069 */
/*global angular:false */
angular.module('', [])
    .factory('RulesApiSwaggerAngularClient', ['$q', '$http', '$rootScope', function($q, $http, $rootScope) {
        'use strict';

        /**
     * This is the API for configuring the demand partner and the supply for those demand
partners.

     * @class RulesApiSwaggerAngularClient
     * @param {(string|object)} [domainOrOptions] - The project domain or options object. If object, see the object's optional properties.
     * @param {string} [domainOrOptions.domain] - The project domain
     * @param {string} [domainOrOptions.cache] - An angularjs cache implementation
     * @param {object} [domainOrOptions.token] - auth token - object with value property and optional headerOrQueryName and isQuery properties
     * @param {string} [cache] - An angularjs cache implementation
     */
        var RulesApiSwaggerAngularClient = (function() {
            function RulesApiSwaggerAngularClient(options, cache) {
                var domain = (typeof options === 'object') ? options.domain : options;
                this.domain = typeof(domain) === 'string' ? domain : 'https://api.ntoggle.com/v1';
                if (this.domain.length === 0) {
                    throw new Error('Domain parameter must be specified as a string.');
                }
                cache = cache || ((typeof options === 'object') ? options.cache : cache);
                this.cache = cache;
            }

            RulesApiSwaggerAngularClient.prototype.$on = function($scope, path, handler) {
                var url = domain + path;
                $scope.$on(url, function() {
                    handler();
                });
                return this;
            };

            RulesApiSwaggerAngularClient.prototype.$broadcast = function(path) {
                var url = domain + path;
                //cache.remove(url);
                $rootScope.$broadcast(url);
                return this;
            };

            RulesApiSwaggerAngularClient.transformRequest = function(obj) {
                var str = [];
                for (var p in obj) {
                    var val = obj[p];
                    if (angular.isArray(val)) {
                        val.forEach(function(val) {
                            str.push(encodeURIComponent(p) + "=" + encodeURIComponent(val));
                        });
                    } else {
                        str.push(encodeURIComponent(p) + "=" + encodeURIComponent(val));
                    }
                }
                return str.join("&");
            };

            /**
             * Adds a new demand partner for configuration management

             * @method
             * @name RulesApiSwaggerAngularClient#postDemandPartners
             * @param {} demandPartner - the properties of the demand partner to be created
             * 
             */
            RulesApiSwaggerAngularClient.prototype.postDemandPartners = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                if (parameters['demandPartner'] !== undefined) {
                    body = parameters['demandPartner'];
                }

                if (parameters['demandPartner'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: demandPartner'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'POST',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * View all demand partners in the system.

             * @method
             * @name RulesApiSwaggerAngularClient#getDemandPartners
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getDemandPartners = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * View demand partner details

             * @method
             * @name RulesApiSwaggerAngularClient#getDemandPartnersByDpId
             * @param {string} dpId - the unique demand partner id
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getDemandPartnersByDpId = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Vieww all supply partners

             * @method
             * @name RulesApiSwaggerAngularClient#getSupplyPartners
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getSupplyPartners = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/supply-partners';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Adds a new supply partner for configuration management

             * @method
             * @name RulesApiSwaggerAngularClient#postSupplyPartners
             * @param {} supplyPartner - the properties of the supply partner to be created
             * 
             */
            RulesApiSwaggerAngularClient.prototype.postSupplyPartners = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/supply-partners';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                if (parameters['supplyPartner'] !== undefined) {
                    body = parameters['supplyPartner'];
                }

                if (parameters['supplyPartner'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: supplyPartner'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'POST',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * View new supply partner details

             * @method
             * @name RulesApiSwaggerAngularClient#getSupplyPartnersBySpId
             * @param {string} spId - the unique supply partner id
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getSupplyPartnersBySpId = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/supply-partners/{spId}';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * View the supply partners configured for a demand partner

             * @method
             * @name RulesApiSwaggerAngularClient#getDemandPartnersByDpIdSupplyPartners
             * @param {string} dpId - the unique demand partner id
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getDemandPartnersByDpIdSupplyPartners = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Configures a new supply partner for the demand partner

             * @method
             * @name RulesApiSwaggerAngularClient#postDemandPartnersByDpIdSupplyPartners
             * @param {string} dpId - the unique demand partner id
             * @param {} supplyPartnerConfiguration - the properties of the supply partner configuration
             * 
             */
            RulesApiSwaggerAngularClient.prototype.postDemandPartnersByDpIdSupplyPartners = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                if (parameters['supplyPartnerConfiguration'] !== undefined) {
                    body = parameters['supplyPartnerConfiguration'];
                }

                if (parameters['supplyPartnerConfiguration'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: supplyPartnerConfiguration'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'POST',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Returns the configuration of supply partner for the demand partner

             * @method
             * @name RulesApiSwaggerAngularClient#getDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeId
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} spTypeId - the type of the supply
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeId = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners/{spId}/types/{spTypeId}';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                path = path.replace('{spTypeId}', parameters['spTypeId']);

                if (parameters['spTypeId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spTypeId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Updates the configuration for the supply partner for the demand partner

             * @method
             * @name RulesApiSwaggerAngularClient#putDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeId
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} spTypeId - the type of the supply
             * 
             */
            RulesApiSwaggerAngularClient.prototype.putDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeId = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners/{spId}/types/{spTypeId}';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                path = path.replace('{spTypeId}', parameters['spTypeId']);

                if (parameters['spTypeId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spTypeId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'PUT',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Return all the version for the specified identifiers.

             * @method
             * @name RulesApiSwaggerAngularClient#getVersions
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} spTypeId - the type of the supply
             * @param {string} limit - the maximum number of items to be returned.
             * @param {string} offset - the offset from the start of the list to be returned.
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getVersions = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/versions';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                if (parameters['dpId'] !== undefined) {
                    queryParameters['dpId'] = parameters['dpId'];
                }

                if (parameters['spId'] !== undefined) {
                    queryParameters['spId'] = parameters['spId'];
                }

                if (parameters['spTypeId'] !== undefined) {
                    queryParameters['spTypeId'] = parameters['spTypeId'];
                }

                if (parameters['limit'] !== undefined) {
                    queryParameters['limit'] = parameters['limit'];
                }

                if (parameters['limit'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: limit'));
                    return deferred.promise;
                }

                if (parameters['offset'] !== undefined) {
                    queryParameters['offset'] = parameters['offset'];
                }

                if (parameters['offset'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: offset'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * View the version specified

             * @method
             * @name RulesApiSwaggerAngularClient#getVersionsByVersion
             * @param {string} version - the configuration version
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getVersionsByVersion = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/versions/{version}';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{version}', parameters['version']);

                if (parameters['version'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: version'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Update the max and desired QPS for rules in the versions

             * @method
             * @name RulesApiSwaggerAngularClient#postVersionsByVersionQps
             * @param {string} version - the configuration version
             * @param {} qpsUpdate - The QPS update JSON you want to post
             * 
             */
            RulesApiSwaggerAngularClient.prototype.postVersionsByVersionQps = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/versions/{version}/qps';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{version}', parameters['version']);

                if (parameters['version'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: version'));
                    return deferred.promise;
                }

                if (parameters['qpsUpdate'] !== undefined) {
                    body = parameters['qpsUpdate'];
                }

                if (parameters['qpsUpdate'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: qpsUpdate'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'POST',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Copy the specified Version to a new Draft Version so it can either be published or edited.

             * @method
             * @name RulesApiSwaggerAngularClient#postVersionsByVersionCreateDraft
             * @param {string} version - the configuration version
             * 
             */
            RulesApiSwaggerAngularClient.prototype.postVersionsByVersionCreateDraft = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/versions/{version}/createDraft';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{version}', parameters['version']);

                if (parameters['version'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: version'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'POST',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Search for feature matches based on a partial feature string and an attribute name

             * @method
             * @name RulesApiSwaggerAngularClient#getDemandPartnersByDpIdSupplyPartnersBySpIdFeaturesSearch
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} attr - the attribute name to search for the feature in
             * @param {string} q - the partial feature name to search for
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getDemandPartnersByDpIdSupplyPartnersBySpIdFeaturesSearch = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners/{spId}/features/search';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                if (parameters['attr'] !== undefined) {
                    queryParameters['attr'] = parameters['attr'];
                }

                if (parameters['attr'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: attr'));
                    return deferred.promise;
                }

                if (parameters['q'] !== undefined) {
                    queryParameters['q'] = parameters['q'];
                }

                if (parameters['q'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: q'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Publish the configuration to the routes for this supply partner

             * @method
             * @name RulesApiSwaggerAngularClient#getDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdPublish
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} spTypeId - the type of the supply
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdPublish = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners/{spId}/types/{spTypeId}/publish';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                path = path.replace('{spTypeId}', parameters['spTypeId']);

                if (parameters['spTypeId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spTypeId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Create a new rule for a supply source and demand partner

             * @method
             * @name RulesApiSwaggerAngularClient#postDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdRules
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} spTypeId - the type of the supply
             * 
             */
            RulesApiSwaggerAngularClient.prototype.postDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdRules = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners/{spId}/types/{spTypeId}/rules';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                path = path.replace('{spTypeId}', parameters['spTypeId']);

                if (parameters['spTypeId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spTypeId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'POST',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Return the rule for a supply source and demand partner

             * @method
             * @name RulesApiSwaggerAngularClient#getDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdRulesByRuleId
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} spTypeId - the type of the supply
             * @param {string} ruleId - the unique rule id
             * 
             */
            RulesApiSwaggerAngularClient.prototype.getDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdRulesByRuleId = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners/{spId}/types/{spTypeId}/rules/{ruleId}';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                path = path.replace('{spTypeId}', parameters['spTypeId']);

                if (parameters['spTypeId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spTypeId'));
                    return deferred.promise;
                }

                path = path.replace('{ruleId}', parameters['ruleId']);

                if (parameters['ruleId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: ruleId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var cached = parameters.$cache && parameters.$cache.get(url);
                if (cached !== undefined && parameters.$refresh !== true) {
                    deferred.resolve(cached);
                    return deferred.promise;
                }
                var options = {
                    timeout: parameters.$timeout,
                    method: 'GET',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Replaces a rule with a new version for a supply source and demand partner

             * @method
             * @name RulesApiSwaggerAngularClient#putDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdRulesByRuleId
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} spTypeId - the type of the supply
             * @param {string} ruleId - the unique rule id
             * 
             */
            RulesApiSwaggerAngularClient.prototype.putDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdRulesByRuleId = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners/{spId}/types/{spTypeId}/rules/{ruleId}';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                path = path.replace('{spTypeId}', parameters['spTypeId']);

                if (parameters['spTypeId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spTypeId'));
                    return deferred.promise;
                }

                path = path.replace('{ruleId}', parameters['ruleId']);

                if (parameters['ruleId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: ruleId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'PUT',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };
            /**
             * Remove the rule for a supply source and demand partner

             * @method
             * @name RulesApiSwaggerAngularClient#deleteDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdRulesByRuleId
             * @param {string} dpId - the unique demand partner id
             * @param {string} spId - the unique supply partner id
             * @param {string} spTypeId - the type of the supply
             * @param {string} ruleId - the unique rule id
             * 
             */
            RulesApiSwaggerAngularClient.prototype.deleteDemandPartnersByDpIdSupplyPartnersBySpIdTypesBySpTypeIdRulesByRuleId = function(parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var deferred = $q.defer();

                var domain = this.domain;
                var path = '/demand-partners/{dpId}/supply-partners/{spId}/types/{spTypeId}/rules/{ruleId}';

                var body;
                var queryParameters = {};
                var headers = {};
                var form = {};

                path = path.replace('{dpId}', parameters['dpId']);

                if (parameters['dpId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: dpId'));
                    return deferred.promise;
                }

                path = path.replace('{spId}', parameters['spId']);

                if (parameters['spId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spId'));
                    return deferred.promise;
                }

                path = path.replace('{spTypeId}', parameters['spTypeId']);

                if (parameters['spTypeId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: spTypeId'));
                    return deferred.promise;
                }

                path = path.replace('{ruleId}', parameters['ruleId']);

                if (parameters['ruleId'] === undefined) {
                    deferred.reject(new Error('Missing required  parameter: ruleId'));
                    return deferred.promise;
                }

                if (parameters.$queryParameters) {
                    Object.keys(parameters.$queryParameters)
                        .forEach(function(parameterName) {
                            var parameter = parameters.$queryParameters[parameterName];
                            queryParameters[parameterName] = parameter;
                        });
                }

                var url = domain + path;
                var options = {
                    timeout: parameters.$timeout,
                    method: 'DELETE',
                    url: url,
                    params: queryParameters,
                    data: body,
                    headers: headers
                };
                if (Object.keys(form).length > 0) {
                    options.data = form;
                    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
                    options.transformRequest = RulesApiSwaggerAngularClient.transformRequest;
                }
                $http(options)
                    .success(function(data, status, headers, config) {
                        deferred.resolve(data);
                        if (parameters.$cache !== undefined) {
                            parameters.$cache.put(url, data, parameters.$cacheItemOpts ? parameters.$cacheItemOpts : {});
                        }
                    })
                    .error(function(data, status, headers, config) {
                        deferred.reject({
                            status: status,
                            headers: headers,
                            config: config,
                            body: data
                        });
                    });

                return deferred.promise;
            };

            return RulesApiSwaggerAngularClient;
        })();

        return RulesApiSwaggerAngularClient;
    }]);