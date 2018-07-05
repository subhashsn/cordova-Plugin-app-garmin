#import <Cordova/CDV.h>
#import <health/GHInitializer.h>
#import <health/health.h>


@interface Garmin : CDVPlugin<GHScanDelegate,GHPairingDelegate,GHSyncDelegate,GHParseOptionsDelegate,GHDeviceConnectionDelegate>

- (void)garminInitializer:(CDVInvokedUrlCommand *)command;
- (void)scanForDevice:(CDVInvokedUrlCommand *)command;
- (void)requestSleepData:(CDVInvokedUrlCommand *)command;
- (void)requestActivityData:(CDVInvokedUrlCommand *)command;
- (void)unpairDevice:(CDVInvokedUrlCommand *)command;




@property (nonatomic, readonly) NSMutableArray<GHRemoteDevice*> *pairedDevices;
@property (nonatomic, strong) GHScannedDevice *device;//(GHSyncResult *)result


@end
