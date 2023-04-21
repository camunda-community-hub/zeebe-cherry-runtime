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

  constructor() {
    console.log("RestCallService: ------------ constructor ");
  }


  static getInstance() {
    // console.log("FactoryService.getInstance")
    return new RestCallService();
  }


  getJson(uri, objToCall, fctToCallback) {
    let headers = {'Content-Type': 'application/json'};

    const requestOptions = {
      headers: headers
    };
    uri = uri + "&timezoneoffset=" + (new Date()).getTimezoneOffset();
    console.log("RestCallService.getJson: uri=" + uri);
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
  postJson(uri, objToCall, param, fctToCallback) {
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
        if (fctToCallback !=null) {
          let httpResponse = new HttpResponse(axiosPayload, null);
          fctToCallback.call(objToCall, httpResponse);
        } else {
          console.log("RestCallService.postJson: No call back defined");
        }
      })
      .catch(error => {
        console.error("RestCallService.getJson: Uri[" + selfUri + "] catch error:" + error);
        if (error.response && error.response.status && error.response.status === 401) {
          let homeTogh = window.location.href;
          console.log("Redirect : to[" + homeTogh + "]");
          window.location = homeTogh;
          return;
        }
        if (fctToCallback !=null) {
          let httpResponse = new HttpResponse({}, error)
          fctToCallback.call(objToCall, httpResponse);
        } else {
          console.log("RestCallService.postJson: No call back defined");
        }


      });
  }

  putJson(uri, objToCall, param, fctToCallback) {
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
        console.log("RestCallService.putJson: payload:"+JSON.stringify(axiosPayload.data));
        if (fctToCallback !=null) {
          let httpResponse = new HttpResponse(axiosPayload, null);
          fctToCallback.call(objToCall, httpResponse);
        } else {
          console.log("RestCallService.postJson: No call back defined");
        }
      })
      .catch(error => {
        console.error("RestCallService.putJson: Uri[" + selfUri + "] catch error:" + error);
        if (error.response && error.response.status && error.response.status === 401) {
          let homeTogh = window.location.href;
          console.log("Redirect : to[" + homeTogh + "]");
          window.location = homeTogh;
          return;
        }
        if (fctToCallback !=null) {
          let httpResponse = new HttpResponse({}, error)
          fctToCallback.call(objToCall, httpResponse);
        } else {
          console.log("RestCallService.postJson: No call back defined");
        }

      });
  }
}

export default RestCallService;