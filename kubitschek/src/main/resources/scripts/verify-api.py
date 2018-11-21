from api_client import ApiClient
import json


# client = ApiClient("http://dca-dev01.ntoggle.com:9000")
client = ApiClient("http://localhost:9000")

def printBanner(msg):
    print("--------------------")
    print "\t" + msg
    print("--------------------")


base_id = client._random_id(10)
print "base: "+base_id

print "AUTH --------------"
print "token: " + json.loads(client.get_token('neil-test', 'T3stM3Now'))["accessToken"]
authenticatedUser = client.get_user(headers=client._auth_header())
print "authenticatedUser: " + authenticatedUser
authenticatedDpId = client._get_dpid(authenticatedUser)
print "authenticated dpId: " + authenticatedDpId
authenticatedSupplyPartners = client.get_user_demand_config(headers=client._auth_header())
print "authenticated SupplyPartners: " + authenticatedSupplyPartners
authenticatedSpIds = client._get_spids(authenticatedSupplyPartners)
# print "authenticated spids: " + authenticatedSpIds

authenticatedSpId = authenticatedSpIds[0]

print "using first spid: " + authenticatedSpId

#########################################################
# These commands are 'admin' and don't use auth yet
#
printBanner("Demand Partners")
dp_id = client._get_id("id", client.create_dp("dsp-"+base_id, headers=client._headers()))
print "dp_id: "+dp_id
print client.get_dp(dp_id)
print client.get_all_dps(5, 0)

printBanner("Exchanges (Supply Partners)")
sp_id = client._get_id("id", client.create_sp("exchange-"+base_id, headers=client._headers()))
print "sp_id: "+sp_id
print client.get_sp(sp_id)
print client.get_all_sps(5, 0)

config = {
    "maxQps":20000,
    "configEndpoint": {
        "host":"ntoggle-python-test.com",
        "port":9000 }

}

printBanner("Configurations")

print client.get_config(authenticatedDpId, authenticatedSpId)

print client.get_all_configs_for_dp(authenticatedDpId)

v_id = client._get_id("vId", client.add_config(dp_id, sp_id, config, headers=client._headers()))
print "New configuration created version id: "+v_id

printBanner("Versions")

#print client.get_version(v_id, headers=client._auth_header())
print client.get_all_versions(10,0, headers=client._auth_header())
print client.get_all_configs_for_dp(dp_id, headers=client._auth_header())
printBanner("Create Empty Version for Authenticated User")
ev = client.create_empty_version(authenticatedSpId, 10017, headers=client._auth_header())
print "Empty Version Id: " + client._get_id("id", ev)
print client.get_all_versions(10,0, headers=client._auth_header())

printBanner("Rules")
rule = {
    "name": "rule-"+client._random_id(10),
    "conditions": {
        "carrier": {
            "default": "allow",
            "undefined": "allow",
            "exceptions": ["AT&T", "Verizon"]
        },
        "handset": {
            "default": "exclude",
            "undefined": "allow",
            "exceptions": ["F536F6D652D4D4647-F536F6D652D4D6F64656C"]
        },
        "os": {
            "default": "exclude",
            "undefined": "allow",
            "exceptions": ["F536F6D652D4F53-F536F6D652D56657273696F6E"]
        },
        "wifi": {
            "default": "exclude",
            "undefined": "exclude",
            "exceptions": []
        },
        "appAndSite": {
            "default": "allow",
            "undefined": "exclude",
            "exceptions": ["e5276ba5-2746-12b5-c64a-1112a5d5c421"]
        },
        "adSize": {
            "default": "exclude",
            "undefined": "exclude",
            "exceptions": ["1024x768"]
        },
        "country": {
            "default": "allow",
            "undefined": "exclude",
            "exceptions": ["US"]
        },
        "region": {
            "default": "exclude",
            "undefined": "exclude",
            "exceptions": ["Some-Region"]
        },
        "city": {
            "default": "allow",
            "undefined": "exclude",
            "exceptions": ["Some-City"]
        },
        "zip": {
            "default": "allow",
            "undefined": "exclude",
            "exceptions": ["02114"]
        }
    }
}
r_id = client._get_id("id", client.save_rule(v_id, rule, client._headers()))
print "rule id: "+ r_id
print client.get_rule(r_id)

rule_upd = {
    "id": r_id,
    "name": "rule-upd-"+client._random_id(10),
    "conditions": {
        "carrier": {
            "default": "allow",
            "undefined": "allow",
            "exceptions": ["AT&T"]
        },
        "handset": {
            "default": "exclude",
            "undefined": "allow",
            "exceptions": ["F536F6D652D4D4647-F536F6D652D4D6F64656C"]
        },
        "os": {
            "default": "allow",
            "undefined": "exclude",
            "exceptions": ["F536F6D652D4F53-F536F6D652D56657273696F6E"]
        },
        "wifi": {
            "default": "exclude",
            "undefined": "exclude",
            "exceptions": []
        },
        "appAndSite": {
            "default": "allow",
            "undefined": "exclude",
            "exceptions": ["e5276ba5-2746-12b5-c64a-1112a5d5c421"]
        },
        "adSize": {
            "default": "exclude",
            "undefined": "exclude",
            "exceptions": ["1024x768"]
        },
        "country": {
            "default": "allow",
            "undefined": "exclude",
            "exceptions": ["US"]
        },
        "zip": {
            "default": "allow",
            "undefined": "exclude",
            "exceptions": ["02114"]
        }
    }
}
client.update_rule(v_id, rule_upd, client._headers())
print client.get_rule(r_id)
print "should only be one (1) rule"
print client.get_version(v_id)
client.save_rule(v_id, rule, client._headers())
print "should only be two (2) rules"
print client.get_version(v_id)


rr_id = client._get_id("id", client.save_rule(ev_id, rule, client._headers()))
print "before remove"
print client.get_version(ev_id)
print "removing rule: "+rr_id
print client.remove_rule(ev_id, rr_id)
print "after remove"
print client.get_version(ev_id)

print "orig:"
print client.get_version(v_id)
cv_id = client._get_id("id", client.copy_version(v_id))
print "copy:"
print client.get_version(cv_id)

print "update qps"
cv = json.loads(client.get_version(cv_id))
qps = {
    "maxQps": 12000,
    "rules": [
        { "id": cv["rules"][0]["id"], "desiredQps": 34000},
        { "id": cv["rules"][1]["id"], "desiredQps": 7000}
    ]
}
client.update_version_qps(cv_id, qps, headers=client._headers())
print "after update"
print client.get_version(cv_id)

print "publish..."
print client.publish(cv_id)

# print "search for feature..."
# print client.get_features(dp_id, sp_id, "web", "country", "U", 10, 0)



