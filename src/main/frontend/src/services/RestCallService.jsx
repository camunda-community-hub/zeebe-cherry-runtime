// -----------------------------------------------------------
//
// RestCallService
//
// RestAPI Calls
//
// -----------------------------------------------------------
import axios from 'axios';

import HttpResponse from './HttpResponse';


class RestCallService {


  static getInstance() {
    // console.log("FactoryService.getInstance")
    return new RestCallService();
  }


  /**
   * The React proxy does not work for Href, so we have to simulate it here
   * proxy is configured in package.json, and it is actif when react is working under localhost:3000
   * "proxy": "http://127.0.0.1:9081"
   * @returns {string}
   */
  getUrlServer() {
    console.log("RestCallService.getUrlServer");
    if (window.location.host === "http://localhost:3000") {
      // put the same value here as in the package.json
      return "http://localhost:9081";
    } else
      return "";
  }


  getJson(uri, objToCall, fctToCallback) {
    let headers = {'Content-Type': 'application/json'};

    const requestOptions = {
      headers: headers
    };
    uri = uri + "&timezoneoffset=" + (new Date()).getTimezoneOffset();
    axios.get(uri, requestOptions)
      .then(axiosPayload => {
        // console.log("RestCallService.getJson: payload:"+JSON.stringify(axiosPayload.data));
        let httpResponse = new HttpResponse(axiosPayload, null);
        fctToCallback.call(objToCall, httpResponse);
      })
      .catch(error => {
        console.log("RestcallService.getJson() error " + error);
        if (error.response && error.response.status === 401) {
          return;
        }
        console.error("RestCallService.getJson: catch error:" + error);
        let httpResponse = new HttpResponse({}, error);
        fctToCallback.call(objToCall, httpResponse);

      });
  }

// PostJson
  postJson(uri, param, objToCall, fctToCallback) {
    let headers = {'Content-Type': 'application/json'};
    param.timezoneoffset = (new Date()).getTimezoneOffset();
    console.log("RestCallService.postJson: timezoneoffset=" + param.timezoneoffset);
    uri = uri + "&timezoneoffset=" + (new Date()).getTimezoneOffset();

    const requestOptions = {
      headers: headers
    };
    var selfUri = uri;
    axios.post(uri, param, requestOptions)
      .then(axiosPayload => {
        // console.log("RestCallService.getJson: payload:"+JSON.stringify(axiosPayload.data));
        if (fctToCallback != null) {
          let httpResponse = new HttpResponse(axiosPayload, null);
          fctToCallback.call(objToCall, httpResponse);
        } else {
          console.log("RestCallService.postJson: No call back defined");
        }
      })
      .catch(error => {
        console.error("RestCallService.getJson: Uri[" + selfUri + "] catch error:" + error);
        if (error.response && error.response.status && error.response.status === 401) {
          let homeCherry = window.location.href;
          console.log("Redirect : to[" + homeCherry + "]");
          window.location = homeCherry;
          return;
        }
        if (fctToCallback != null) {
          let httpResponse = new HttpResponse({}, error)
          fctToCallback.call(objToCall, httpResponse);
        } else {
          console.log("RestCallService.postJson: No call back defined");
        }


      });
  }

  putJson(uri, param, objToCall, fctToCallback) {
    let headers = {'Content-Type': 'application/json'};
    param.timezoneoffset = (new Date()).getTimezoneOffset();
    console.log("RestCallService.putJson: timezoneoffset=" + param.timezoneoffset);
    uri = uri + "&timezoneoffset=" + (new Date()).getTimezoneOffset();

    const requestOptions = {
      headers: headers
    };
    var selfUri = uri;
    axios.put(uri, param, requestOptions)
      .then(axiosPayload => {
        console.log("RestCallService.putJson: payload:" + JSON.stringify(axiosPayload.data));
        if (fctToCallback != null) {
          let httpResponse = new HttpResponse(axiosPayload, null);
          fctToCallback.call(objToCall, httpResponse);
        } else {
          console.log("RestCallService.putJson: No call back defined");
        }
      })
      .catch(error => {
        console.error("RestCallService.putJson: Uri[" + selfUri + "] catch error:" + error);
        if (error.response && error.response.status && error.response.status === 401) {
          let homeCherry = window.location.href;
          console.log("Redirect : to[" + homeCherry + "]");
          window.location = homeCherry;
          return;
        }
        if (fctToCallback != null) {
          let httpResponse = new HttpResponse({}, error)
          fctToCallback.call(objToCall, httpResponse);
        } else {
          console.log("RestCallService.putJson: No call back defined");
        }

      });
  }
}

export default RestCallService;