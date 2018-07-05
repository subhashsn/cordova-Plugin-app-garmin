#import "Garmin.h"
#import <Cordova/CDV.h>
#import <objc/runtime.h>



@implementation Garmin
{
    NSDictionary *resultDict;
    CDVInvokedUrlCommand *invokedUrlCommand;
    NSString *BLPassCode;
}

// Garmin health SDK plugin methods
- (void)garminInitializer:(CDVInvokedUrlCommand *)command
{
    GHInitializer *initializer = [GHInitializer sharedManager];
    [initializer initializeLicense:@"CgoCAwQFBgcICQoLEoACamC+igePkGXooePHOuKV0JpAn4id4FJM9BHAPaC3Qay4OAsaKGJUcd8fFJwwZLUMMsHlrQQ6+lJpvHXovXjLc8rpv+uj50JHqDW2JOIyxmsq4+SdwDoVBdoSnpsDU36V7NvZlNuHFq1Rv/UNjfHwBL0dWliPH6H1j9Mheo93Hq3leHspR9ewG4xzBQxHIGu08LBzjD+LQl6c6RCm6qB+efpDi22D9dTdTI78dvzpzQ3ft8EFDhG72lj4pIy+TcFhvNuGYqGgS3ul5pN9+/L0G8HNefssIpT3aMNZM/ZxJmlClGxSnMYYEJCIpF1hs5/Q42mmXVNwCPG5LT0WcuTFHhiA8JCvwCw="];
    [initializer initializeClientID:@"x3AtHNPxTVdApMGy6ZMiHiIGFTg6mcZd" secret:@"yxrLo5nqUzLxicbd"];
    
    // Keep data from being deleted on the watch during development.
    [initializer setKeepData:NO];
    
    // Don't check the version of the firmware
    [initializer setBypassFirmwareVersionValidation:NO];
    
    //[self setupSyncDelegate];
    
    NSString *message = [NSString stringWithFormat:@"Garmin SDK is initialized"];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    
    //[self requestSleepData:command];
}

- (void)scanForDevice:(CDVInvokedUrlCommand *)command
{
        NSDictionary* passcode = [[command arguments] objectAtIndex:0];
        NSLog(@"passcode %@",passcode);
        invokedUrlCommand=command;
        GHRemoteDeviceManager* deviceManager = [GHRemoteDeviceManager sharedInstance];
        [deviceManager setScanDelegate:self];
        //[self setupSyncDelegate];
        //start scanning for devices
    
        BLPassCode=[NSString stringWithFormat:@"%@",[passcode valueForKey:@"passcode"]];
        [deviceManager scanForDevices:GHDeviceTypeAll];
        [self.pairedDevices addObjectsFromArray:deviceManager.getPairedDevices];
        NSLog(@"pairedDevices:-%@",self.pairedDevices);
        NSString *message = [NSString stringWithFormat:@"Scanning for device"];
        
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    
    
}

#pragma for Scanning with BT
- (IBAction)forget:(id)sender {
    
    GHRemoteDeviceManager *mgr = [GHRemoteDeviceManager sharedInstance];
    [mgr forgetDevice:[self.pairedDevices objectAtIndex:0].identifier];
}

-(void)didScanDevice:(GHScannedDevice *)device {
    NSString *javascript1;
    NSString *message;
    
    if ([BLPassCode isEqualToString:@"(null)"]) {
        // Craft JavaScript call
        message = [NSString stringWithFormat:@"Your device is not paired,Please pair before sync"];
        javascript1 = [NSString stringWithFormat: @"window.handleError('%@')", message];
        NSLog(@"javascript1 %@",javascript1);
        [self scanForDevice:nil];
//        if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
//        {
//            [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
//        } else{
//            [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
//        }
        
    }
    else if ([BLPassCode isEqualToString:device.passcode])
    {
        [self.pairedDevices addObject:device];
        self.device=device;
        [self pair:device];
        NSLog(@"didScanDevice %@",device);
        BLPassCode=nil;
        // Craft JavaScript call
        
//         message = [NSString stringWithFormat:@"Your passcode validated successfully"];
//         javascript1= [NSString stringWithFormat: @"window.handlePairing('%@')", message];
//        NSLog(@"javascript1 %@",javascript1);
        
    }
    else
    {
        // Craft JavaScript call
        message = [NSString stringWithFormat:@"Your passcode is wrong.Please enter correct passcode"];
        javascript1 = [NSString stringWithFormat: @"window.handleError('%@')", message];
        NSLog(@"javascript1 %@",javascript1);
        if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
        {
            [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
        } else{
            [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
        }
    }
    
    
    
}

-(void)scanDidFailWithError:(NSError *)error {
    //[self showError:@"Scanning failed"];
    NSLog(@"scanDidFailWithError");
    
     GHRemoteDeviceManager* deviceManager = [GHRemoteDeviceManager sharedInstance];
    [deviceManager stopScan];
    //java script call
   /*NSString *msg=[error.userInfo valueForKey:@"reason"];
    
    NSString *javascript1 = [NSString stringWithFormat: @"window.handleError('%@')", @"Scan failed,please switch on bluetooth."];
    
    if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
    {
        [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
    } else{
        [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
    }*/
}

#pragma for pairing with BT
- (void)pair:(GHScannedDevice*)scannedDevice {
    GHRemoteDeviceManager *mgr = [GHRemoteDeviceManager sharedInstance];
    //TODO: Should be customizable in UI
    GHMutableUserSettings *userSettings = [GHMutableUserSettings new];
    userSettings.gender = GHUserGenderMale;
    userSettings.age = 55;
    userSettings.height = 1.75;
    userSettings.weight = 77;
    
    [mgr pairDevice:scannedDevice userSettings:userSettings delegate:self];
    NSLog(@"pairing %@",scannedDevice);
}
- (void)didPairDevice:(GHRemoteDevice *)device {
    //save device
    // un-comment for auto sync
    /*NSError *error;
    
    GHMutableDeviceSettings *devSeting=(GHMutableDeviceSettings*)device.settings;
    [devSeting setAutoSyncFrequency:GHSyncFrequencyCustom];
    [devSeting setAutoUploadEnabled:YES];
    [devSeting setAutoSyncCustomSteps:[NSNumber numberWithInt:100]];
    [devSeting setAutoSyncCustomTime:[NSNumber numberWithInt:10]];
    [device updateSettings:devSeting error:&error];*/
    
    NSMutableDictionary *tempDict=[NSMutableDictionary new];
    [tempDict setValue:device.friendlyName forKey:@"friendlyName"];
    [tempDict setValue:[NSString stringWithFormat:@"%@",device.unitID] forKey:@"unitID"];
    [tempDict setValue:[NSString stringWithFormat:@"%@",device.identifier] forKey:@"deviceId"];
    [tempDict setValue:[NSString stringWithFormat:@"%d",device.settings.userSettings.age] forKey:@"age"];
    [tempDict setValue:[NSString stringWithFormat:@"%f",device.settings.userSettings.height] forKey:@"height"];
    [tempDict setValue:[NSString stringWithFormat:@"%f",device.settings.userSettings.weight] forKey:@"weight"];
    [tempDict setValue:[NSString stringWithFormat:@"%hhu",device.settings.userSettings.gender] forKey:@"gender"];
    
    NSData *data = [NSKeyedArchiver archivedDataWithRootObject:tempDict];
    
    [[NSUserDefaults standardUserDefaults] setObject:data forKey:@"userDetails"];

    [resultDict setValue:tempDict forKey:@"userDetails"];
    
    // javascript call
    [tempDict setValue:@"Paired" forKey:@"status"];
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:tempDict
                                                       options:NSJSONReadingAllowFragments
                                                         error:&error];
    
    if (! jsonData) {
        NSLog(@"Got an error: %@", error);
    } else {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        [[NSUserDefaults standardUserDefaults] setObject:jsonString forKey:@"jsonData"];
        NSLog(@"%@",jsonString);
        
        
        // Serialise to JSON string
        // Craft JavaScript call
        NSString *javascript1 = [NSString stringWithFormat: @"window.handlePairing(%@)", jsonString];
        NSLog(@"javascript1 %@",javascript1);
        //[self getSyncData:invokedUrlCommand];
        
        if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
        {
            [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
        } else{
            [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
        }
    }
    
    // stop scan
    GHRemoteDeviceManager* deviceManager = [GHRemoteDeviceManager sharedInstance];
    [deviceManager stopScan];
    
    NSLog(@"didPairDevice %@",device);
    
}

- (void)didFailToPairDevice:(NSUUID *)deviceId error:(NSError *)error {
    //[self showError:@"Pairing Failed"];
    
    // stop scan
    NSLog(@"didFailToPairDevice");
    GHRemoteDeviceManager* deviceManager = [GHRemoteDeviceManager sharedInstance];
    [deviceManager stopScan];
    
    //java script call
    /*NSString *javascript1 = [NSString stringWithFormat: @"window.handleError('%@')", @"Pairing failed,please switch on bluetooth."];
    
    if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
    {
        [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
    } else{
        [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
    }*/
}

- (void)didPausePairing:(GHPairingCompletion *)completion {
    [completion complete:self];
    NSLog(@"didPausePairing %@",completion);
}

- (NSDictionary *) dictionaryWithPropertiesOfObject:(id)obj
{
    NSMutableDictionary *dict = [NSMutableDictionary dictionary];
    unsigned count;
    objc_property_t *properties = class_copyPropertyList([obj class], &count);
    
    for (int i = 0; i < count; i++) {
        NSString *key = [NSString stringWithUTF8String:property_getName(properties[i])];
        if ([obj valueForKey:key]) {
            NSString *str=[NSString stringWithFormat:@"%@",[obj valueForKey:key]];
            NSString *finalStr=[str stringByReplacingOccurrencesOfString:@"\n" withString:@""];
            
            [dict setObject:finalStr forKey:key];
        }
    }
    free(properties);
    return [NSDictionary dictionaryWithDictionary:dict];
}

- (NSDictionary *)dictionaryWithObjects:(id)obj {
    unsigned int count = 0;
    NSMutableDictionary *dictionary = [NSMutableDictionary new];
    objc_property_t *properties = class_copyPropertyList([obj class], &count);
    
    for (int i = 0; i < count; i++) {
        
        NSString *key = [NSString stringWithUTF8String:property_getName(properties[i])];
        id value = [obj valueForKey:key];
        
        if (value == nil) {
            // nothing todo
        }
        else if ([value isKindOfClass:[NSArray class]]) {
            // TODO: extend to other types
            NSMutableArray *ary=[NSMutableArray new];
            for (id obj in value) {
                if([obj isKindOfClass:[NSArray class]])
                {
                    NSMutableArray *ary1=[NSMutableArray new];
                    for (id subObj in obj) {
                        if ([subObj isKindOfClass:[NSObject class]]) {
                            NSDictionary *dict1=[self dictionaryWithPropertiesOfObject:subObj];
                            [ary1 addObject:dict1];
                        }
                    }
                    [dictionary setObject:ary1 forKey:key];
                    
                }
                else if ([obj isKindOfClass:[NSObject class]]) {
                    
                    NSDictionary *dict=[self dictionaryWithPropertiesOfObject:obj];
                    
                    [ary addObject:dict];
                    
                }
                else
                {
                    [dictionary setObject:obj forKey:key];
                }
                [dictionary setObject:ary forKey:key];
                
            }
            
            
        }
        else if ([value isKindOfClass:[NSObject class]]) {
            NSDictionary *dict1=[self dictionaryWithPropertiesOfObject:value];
            
            [dictionary setObject:dict1 forKey:key];
        }
        else {
            NSLog(@"Invalid type for %@ (%@)", NSStringFromClass([obj class]), key);
            [dictionary setObject:value forKey:key];
        }
    }
    free(properties);
    return dictionary;
}

#pragma parse option
- (GHIntervalDuration)wellnessIntervalForDevice:(NSUUID *)deviceId
{
    //NSLog(@"%u",GHIntervalDurationTwoMinutes);
    return GHIntervalDurationTwoMinutes;
}
- (GHIntervalDuration)heartRateIntervalForDevice:(NSUUID *)deviceId
{
    //NSLog(@"%u",GHIntervalDurationTwoMinutes);
    return GHIntervalDurationTwoMinutes;
}
#pragma connection delegate
- (void)didConnectDevice:(GHRemoteDevice *)device
{
    
}

#pragma sync delegate
- (void)setupSyncDelegate {
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    
    GHRemoteDeviceManager *deviceMgr = [GHRemoteDeviceManager sharedInstance];
        [deviceMgr removeSyncDelegate:self];
        [deviceMgr addSyncDelegate:self];
        [deviceMgr setParseOptionsDelegate:self];
        [deviceMgr addConnectionDelegate:self];
    });
}

- (void)didStartSyncWithDevice:(NSUUID *)deviceId
{
    
}
- (void)didReceiveSyncProgress:(double)progress deviceId:(NSUUID *)deviceId
{
    
}
- (void)didReceiveSyncResult:(GHSyncResult *)result deviceId:(NSUUID *)deviceId
{
    
    // remove sync delegate
    GHRemoteDeviceManager *deviceMgr = [GHRemoteDeviceManager sharedInstance];
    [deviceMgr removeSyncDelegate:self];
    NSMutableArray *resultAray;
    
    if([[NSUserDefaults standardUserDefaults] valueForKey:@"syncResult"])
    {
        NSData *dataACH = (NSData*)[[NSUserDefaults standardUserDefaults] objectForKey:@"syncResult"];
        resultAray = [NSKeyedUnarchiver unarchiveObjectWithData:dataACH];
        
    }else{
        
        resultAray = [NSMutableArray new];
        
    }
    [resultAray addObject:result];
    //[[NSUserDefaults standardUserDefaults] setObject:resultAray forKey:@"syncResult"];
    
    NSData *data = [NSKeyedArchiver archivedDataWithRootObject:resultAray];
    
    [[NSUserDefaults standardUserDefaults] setObject:data forKey:@"syncResult"];
    [[NSUserDefaults standardUserDefaults]setValue:@"0" forKey:@"index"];
    
    resultDict=[self dictionaryWithObjects:result];
    NSLog(@"%@",resultDict);
    
    NSMutableDictionary *fitData = [NSMutableDictionary new];
    NSMutableArray *tempAry =[NSMutableArray new];
    if ([result.fitData isKindOfClass:[NSObject class]]) {
        for (GHActivityData *object in result.fitData) {
            NSDictionary *dict=[self dictionaryWithObjects:object];
            [tempAry addObject:dict];
        }
        [fitData setObject:tempAry forKey:@"fitData"];
        [resultDict setValuesForKeysWithDictionary:fitData];
    }
    
    NSData *dataACH = [[NSUserDefaults standardUserDefaults] objectForKey:@"userDetails"];
    NSDictionary *userDetails = (NSDictionary *)[NSKeyedUnarchiver unarchiveObjectWithData:dataACH];
    [resultDict setValue:userDetails forKey:@"userDetails"];
    NSLog(@"%@",resultDict);
    
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:resultDict
                                                       options:NSJSONReadingAllowFragments
                                                         error:&error];
    
    if (! jsonData) {
        NSLog(@"Got an error: %@", error);
    } else {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        [[NSUserDefaults standardUserDefaults] setObject:jsonString forKey:@"jsonData"];
        NSLog(@"%@",jsonString);
        
        
        // Serialise to JSON string
        // Craft JavaScript call
        NSString *javascript1 = [NSString stringWithFormat: @"window.handleActivityData(%@)", jsonString];
        NSLog(@"javascript1 %@",javascript1);
        //[self getSyncData:invokedUrlCommand];
        
        if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
        {
            [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
        } else{
                [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
        }
    }
    
}
- (void)didReceiveSyncFiles:(NSArray<NSURL *>*)files deviceId:(NSUUID *)deviceId
{
    
}
- (void)didCompleteSyncWithDevice:(NSUUID *)deviceId error:(NSError *)error
{
    /*GHRemoteDeviceManager* deviceManager = [GHRemoteDeviceManager sharedInstance];
    GHRemoteDevice *device = [deviceManager.getPairedDevices objectAtIndex:0];
    
    [device checkForFirmwareUpdate:^(GHFirmwareResult *result, GHFirmwareStatus status) {
        
        NSLog(@"%@",result);
    }];*/
}
- (void)didStallSyncWithDevice:(NSUUID *)deviceId completion:(void(^)(BOOL stop))shouldStop
{
    
}

// sleep data implementation
- (void)requestSleepData:(CDVInvokedUrlCommand *)command
{
    NSData *dataACH = (NSData*)[[NSUserDefaults standardUserDefaults] objectForKey:@"syncResult"];
    NSMutableArray *resultArrya=[NSKeyedUnarchiver unarchiveObjectWithData:dataACH];
    GHSyncResult *result;
    __block int index=[[[NSUserDefaults standardUserDefaults]valueForKey:@"index"] intValue];
    if (resultArrya.count>0 && resultArrya.count>index) {
      result=[resultArrya objectAtIndex:index];
    }else{
        result=nil;
    }
    
        // for sleep data
        GHSleepRequest *request = [[GHSleepRequest alloc] initWithSyncResult:result];
        if (request) {
            [request requestSleepIntervals:^(NSArray<GHSleepData *> *response, NSError *error) {
                
                if (error) {
                    NSLog(@"Error fetching sleep: %@", error);
                    // Craft JavaScript call
                    NSString *javascript1 = [NSString stringWithFormat: @"window.handleSleepError('%@')", @"Error fetching sleep"];
                    NSLog(@"javascript1 %@",javascript1);
                    index=index+1;
                    [[NSUserDefaults standardUserDefaults]setObject:[NSString stringWithFormat:@"%d",index] forKey:@"index"];
                
                    
                    if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
                    {
                        [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
                    } else{
                        [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
                    }
                }
                else{
                    
                    
                    NSLog(@"%@",response);
                    NSMutableArray *sleepArray=[NSMutableArray new];
                    for (GHSleepData *sleep in response)
                    {
                        NSDictionary *dict=[self dictionaryWithPropertiesOfObject:sleep];
                        [sleepArray addObject:dict];
                    }
                    NSLog(@"%@",sleepArray);
                    
                    NSMutableDictionary *finalDict=[NSMutableDictionary new];
                    [finalDict setValue:sleepArray forKey:@"sleepData"];
                    NSData *dataACH = [[NSUserDefaults standardUserDefaults] objectForKey:@"userDetails"];
                    NSDictionary *userDetails = (NSDictionary *)[NSKeyedUnarchiver unarchiveObjectWithData:dataACH];
                    [finalDict setValue:userDetails forKey:@"userDetails"];
                    
                    NSError *error;
                    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:finalDict options:NSJSONReadingAllowFragments error:&error];
                    if (! jsonData) {
                        NSLog(@"Got an error: %@", error);
                    } else {
                        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
                        [[NSUserDefaults standardUserDefaults] setObject:jsonString forKey:@"jsonData"];
                        NSLog(@"%@",jsonString);
                        
                        // Craft JavaScript call
                        NSString *javascript1 = [NSString stringWithFormat: @"window.handleSleepData(%@)", jsonString];
                        NSLog(@"javascript1 %@",javascript1);
                        
                        if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
                        {
                            [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
                        } else{
                            [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
                        }
                        
                        //[[NSUserDefaults standardUserDefaults] removeObjectForKey:@"syncResult"];
                        
                        if (resultArrya.count > 0) {
                            [resultArrya removeObjectAtIndex:index];
                            NSData *data = [NSKeyedArchiver archivedDataWithRootObject:resultArrya];
                            [[NSUserDefaults standardUserDefaults] setObject:data forKey:@"syncResult"];
                            
                            
                        }
                    }
                }
                [self requestSleepData:command];
            }];
        }
        

    NSString *message = [NSString stringWithFormat:@"Requesting for sleep data."];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

- (void)requestActivityData:(CDVInvokedUrlCommand *)command{
    
    [self setupSyncDelegate];
    NSString *message = [NSString stringWithFormat:@"Requesting for activity data."];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

- (void)unpairDevice:(CDVInvokedUrlCommand *)command
{
    NSDictionary* deviceDict = [[command arguments] objectAtIndex:0];
    NSString *deviceIdentifier=[NSString stringWithFormat:@"%@",[deviceDict valueForKey:@"deviceId"]];
    GHRemoteDeviceManager *mgr = [GHRemoteDeviceManager sharedInstance];
    NSArray *pairedArray=[mgr getPairedDevices];
    
    for (GHRemoteDevice *device in pairedArray) {
        NSString *strIdentifier=[NSString stringWithFormat:@"%@",device.identifier];
        if ([deviceIdentifier isEqualToString:strIdentifier]) {
            [mgr forgetDevice:device.identifier];
            // Craft JavaScript call
           NSString *message = [NSString stringWithFormat:@"unPaired"];
           NSString *javascript1 = [NSString stringWithFormat: @"window.handleUnPairing('%@')", message];
            NSLog(@"javascript1 %@",javascript1);
            if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)])
            {
                [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:javascript1];
            } else{
                [self.webViewEngine evaluateJavaScript:javascript1 completionHandler:nil];
            }
            
        }
    }
}



@end
