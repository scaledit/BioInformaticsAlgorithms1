import urllib2
import urllib
import json
import random, string


class ApiClient:

    def __init__(self, host, debug = False):
        self._baseHostAndPath = host + "/api/v1"
        if( debug):
            handler=urllib2.HTTPHandler(debuglevel=1)
            opener = urllib2.build_opener(handler)
            urllib2.install_opener(opener)

    def _random_id(self, length):
        return ''.join(random.choice(string.lowercase) for i in range(length))

    def _headers(self):
        return {
            'Content-Type': 'application/json'
        }

    def _auth_header(self):
        return {
            'Authorization': 'Bearer '+self._token
        }

    def _delete(self, path, headers = {}):
        url = self._baseHostAndPath + path
        request = urllib2.Request(url, headers=headers)
        request.get_method = lambda: 'DELETE'
        return urllib2.urlopen(request)

    def _post(self, path, data, headers = {}):
        url = self._baseHostAndPath + path
        request = urllib2.Request(url, json.dumps(data), headers=headers)
        return urllib2.urlopen(request)

    def _get(self, path, headers = {}):
        url = self._baseHostAndPath + path
        request = urllib2.Request(url, headers = headers)
        return urllib2.urlopen(request)

    def _get_id(self, id_name, resp):
        return json.loads(resp)[id_name]

    def _get_dpid(self, resp):
        return json.loads(resp)['organization']['id']['dpId']

    def _get_spids(self, resp):
        return [e['id'] for e in json.loads(resp)['supplyPartners']]

    def _get_spids2(self, resp):
        return json.loads(resp)['supplyPartners'][0]['id']

    def get_token(self, username, password):
        url = self._baseHostAndPath + "/oauth2/token"
        data = urllib.urlencode({'username': username, 'password': password, 'grant_type': 'password'})
        request = urllib2.Request(url, data=data)
        result =  urllib2.urlopen(request).read()
        self._token = json.loads(result)["accessToken"]
        return result

    def get_user(self, headers = {}):
        path = "/user"
        return self._get(path, headers = headers).read()

    def get_user_demand_config(self, headers = {}):
        path = "/user/configuration/demand-partner"
        return self._get(path, headers = headers).read()

    def create_dp(self, dp_name, headers = {}):
        path = "/demand-partners"
        return self._post(path=path, data={'name':dp_name}, headers=headers).read()

    def get_dp(self, dp_id, headers = {}):
        path = "/demand-partners/"+dp_id
        return self._get(path, headers).read()

    def get_all_dps(self, limit, offset, headers = {}):
        path = "/demand-partners?limit="+str(limit)+"&offset="+str(offset)
        return self._get(path, headers).read()

    def create_sp(self, sp_name, headers = {}):
        path = "/supply-partners"
        return self._post(path, {'name':sp_name}, headers).read()

    def get_sp(self, sp_id, headers = {}):
        path = "/supply-partners/"+sp_id
        return self._get(path, headers).read()

    def get_all_sps(self, limit, offset, headers = {}):
        path = "/supply-partners?limit="+str(limit)+"&offset="+str(offset)
        return self._get(path, headers).read()

    def add_config(self, dp_id, sp_id, config, headers = {}):
        path = "/demand-partners/"+dp_id+"/supply-partners/" + sp_id
        return self._post(path, config, headers).read()

    def get_config(self, dp_id, sp_id, headers = {}):
        path = "/demand-partners/"+dp_id+"/supply-partners/" + sp_id
        return self._get(path, headers).read()

    def get_all_configs_for_dp(self, dp_id, headers = {}):
        path = "/demand-partners/"+dp_id+"/supply-partners"
        return self._get(path, headers).read()

    def get_version(self, v_id, headers = {}):
        path = "/versions/"+v_id
        return self._get(path, headers).read()

    def create_empty_version(self, sp_id, max_qps, headers = {}):
        path = "/versions/create"
        data = {
            "spId": sp_id,
            "maxQps": max_qps
        }
        return self._post(path, data, headers).read()

    def get_all_versions(self, limit, offset, headers = {}):
        path = "/versions?limit="+str(limit)+"&offset="+str(offset)
        return self._get(path, headers).read()

    def get_rule(self, r_id, headers = {}):
        path = "/rules/"+r_id
        return self._get(path, headers).read()

    def save_rule(self, v_id, rule, headers = {}):
        path = "/versions/"+v_id+"/rule/add"
        return self._post(path, rule, headers=headers).read()

    def update_rule(self, v_id, rule, headers = {}):
        path = "/versions/"+v_id+"/rule/replace"
        return self._post(path, rule, headers=headers).read()

    def remove_rule(self, v_id, r_id, headers = {}):
        path = "/versions/"+v_id+"/rule/remove/"+r_id
        return self._delete(path, headers=headers).read()

    def copy_version(self, v_id, headers = {}):
        path = "/versions/"+v_id+"/copy"
        return self._post(path, headers).read()

    def update_version_qps(self, v_id, data, headers = {}):
        path = "/versions/"+v_id+"/qps"
        return self._post(path, data, headers).read()

    def publish(self, v_id, headers = {}):
        path = "/versions/"+v_id+"/publish"
        return self._post(path, headers).read()

    def get_features(self, dp_id, sp_id, attr, feature, limit, offset, headers = {}):
        path = "/features?dpId="+dp_id+"&spId="+sp_id+"&spTypeId="+type+"&limit="+str(limit)+"&offset="+str(offset)+"&attr="+attr+"&q="+feature
        return self._get(path, headers).read()


