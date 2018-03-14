package jack.fluids.cl;

import org.jocl.*;

import static org.jocl.CL.*;

public class JOCLUtils {
  /**
   * Returns the value of the device info parameter with the given name
   *
   * @param device    The device
   * @param paramName The parameter name
   * @return The value
   */
  public static String getStringDeviceParam(final cl_device_id device,
                                             final int paramName) {
    // Obtain the length of the string that will be queried
    long size[] = new long[1];
    clGetDeviceInfo(device, paramName, 0, null, size);

    // Create a buffer of the appropriate size and fill it with the info
    byte buffer[] = new byte[(int) size[0]];
    clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer),
        null);

    // Create a string from the buffer (excluding the trailing \0 byte)
    return new String(buffer, 0, buffer.length - 1);
  }

  public static String obtainBuildLogs(cl_program program) {
    int numDevices[] = new int[1];
    clGetProgramInfo(program, CL_PROGRAM_NUM_DEVICES, Sizeof.cl_uint, Pointer.to(numDevices), null);
    cl_device_id devices[] = new cl_device_id[numDevices[0]];
    clGetProgramInfo(program, CL.CL_PROGRAM_DEVICES, numDevices[0] * Sizeof.cl_device_id, Pointer.to(devices), null);

    StringBuffer sb = new StringBuffer();
    for (int i=0; i<devices.length; i++) {
      sb.append("Build log for device "+i+":\n");
      long logSize[] = new long[1];
      CL.clGetProgramBuildInfo(program, devices[i], CL.CL_PROGRAM_BUILD_LOG, 0, null, logSize);
      byte logData[] = new byte[(int)logSize[0]];
      CL.clGetProgramBuildInfo(program, devices[i], CL.CL_PROGRAM_BUILD_LOG, logSize[0], Pointer.to(logData), null);
      sb.append(new String(logData, 0, logData.length-1));
      sb.append("\n");
    }
    return sb.toString();
  }

  public static String getEventStatus(cl_event event) {
    long[] result = new long[1];
    clGetEventInfo(event, CL_EVENT_COMMAND_EXECUTION_STATUS, Sizeof.cl_ulong, Pointer.to(result), null);
    return CL.stringFor_errorCode((int) result[0]);
  }

  public static void check(int[] errorCode) {
    check(errorCode[0]);
  }

  public static void check(int errorCode) {
    if (errorCode != CL_SUCCESS) {
      throw new RuntimeException(stringFor_errorCode(errorCode));
    }
  }
}
