//=========================================================================================
// Copyright (c) 2019-2026 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
//=========================================================================================
package vendor.qti.hardware.rgbw;

//@VintfStability
interface IVendorRgbwControl {
    void setLedColor(in int r, in int g, in int b, in int w);
}
