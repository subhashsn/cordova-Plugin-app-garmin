"use strict";
function Garmin() {}

Garmin.prototype.FLAG_ACTIVITY_NEW_TASK = 0x10000000;

Garmin.prototype.canLaunch = function (options, successCallback, errorCallback) {
	options = options || {};
	options.successCallback = options.successCallback || successCallback;
	options.errorCallback = options.errorCallback || errorCallback;
	cordova.exec(options.successCallback || null, options.errorCallback || null, "Garmin", "canLaunch", [options]);
};

Garmin.prototype.launch = function(options, successCallback, errorCallback) {
	options = options || {};
	options.successCallback = options.successCallback || successCallback;
	options.errorCallback = options.errorCallback || errorCallback;
	cordova.exec(options.successCallback || null, options.errorCallback || null, "Garmin", "launch", [options]);
};

/* 
Garmin.prototype.garminInitializer = function (options, successCallback, errorCallback) {
	options = options || {};
	options.successCallback = options.successCallback || successCallback;
	options.errorCallback = options.errorCallback || errorCallback;
	cordova.exec(options.successCallback || null, options.errorCallback || null, "Garmin", "garminInitializer", [options]);
};

Garmin.prototype.scanForDevice = function(options, successCallback, errorCallback) {
	options = options || {};
	options.successCallback = options.successCallback || successCallback;
	options.errorCallback = options.errorCallback || errorCallback;
	cordova.exec(options.successCallback || null, options.errorCallback || null, "Garmin", "scanForDevice", [options]);
};

Garmin.prototype.getSyncData = function(options, successCallback, errorCallback) {
	options = options || {};
	options.successCallback = options.successCallback || successCallback;
	options.errorCallback = options.errorCallback || errorCallback;
	cordova.exec(options.successCallback || null, options.errorCallback || null, "Garmin", "getSyncData", [options]);
};

Garmin.prototype.requestSleepData = function(options, successCallback, errorCallback) {
	options = options || {};
	options.successCallback = options.successCallback || successCallback;
	options.errorCallback = options.errorCallback || errorCallback;
	cordova.exec(options.successCallback || null, options.errorCallback || null, "Garmin", "requestSleepData", [options]);
}; */

Garmin.prototype.scanForDevice = function(options, successCallback, errorCallback) {
		cordova.exec(successCallback , errorCallback , "Garmin", "scanForDevice", [options]);

};

Garmin.prototype.requestSleepData = function(successCallback, errorCallback) {

	cordova.exec(successCallback , errorCallback , "Garmin", "requestSleepData", []);
};

Garmin.prototype.requestActivityData = function(successCallback, errorCallback) {

	cordova.exec(successCallback , errorCallback , "Garmin", "requestActivityData", []);
};

Garmin.prototype.garminInitializer = function(options, successCallback, errorCallback) {

	cordova.exec(successCallback , errorCallback , "Garmin", "garminInitializer", []);
};

Garmin.prototype.unpairDevice = function(options, successCallback, errorCallback) {

	cordova.exec(successCallback , errorCallback , "Garmin", "unpairDevice", [options]);
};


Garmin.install = function () {
	if (!window.plugins) {
		window.plugins = {};
	}

	window.plugins.garmin = new Garmin();
	return window.plugins.garmin;
};

cordova.addConstructor(Garmin.install);
