package com.wg.edftest.EDFlib;/*
*****************************************************************************
*
* Copyright (c) 2020 Teunis van Beelen
* All rights reserved.
*
* Email: teuniz@protonmail.com
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the copyright holder nor the names of its
*       contributors may be used to endorse or promote products derived from
*       this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ''AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
*****************************************************************************
*/


import java.io.*;
import java.nio.*;
import java.lang.*;
import java.util.*;


/**
 * EDF header. <br>
 *<br>
 *<pre>
 * offset (hex, dec) length
 * ---------------------------------------------------------------------
 * 0x00      0     8 ascii : version of this data format (0)
 * 0x08      8    80 ascii : local patient identification
 * 0x58     88    80 ascii : local recording identification
 * 0xA8    168     8 ascii : startdate of recording (dd.mm.yy)
 * 0xB0    176     8 ascii : starttime of recording (hh.mm.ss)
 * 0xB8    184     8 ascii : number of bytes in header record
 * 0xC0    192    44 ascii : reserved
 * 0xEC    236     8 ascii : number of data records (-1 if unknown)
 * 0xF4    244     8 ascii : duration of a data record, in seconds
 * 0xFC    252     4 ascii : number of signals
 *
 *
 *
 *      0x00           0     ns * 16 ascii : ns * label (e.g. EEG Fpz-Cz or Body temp)
 * ns * 0x10    ns *  16     ns * 80 ascii : ns * transducer type (e.g. AgAgCl electrode)
 * ns * 0x60    ns *  96     ns *  8 ascii : ns * physical dimension (e.g. uV or degreeC)
 * ns * 0x68    ns * 104     ns *  8 ascii : ns * physical minimum (e.g. -500 or 34)
 * ns * 0x70    ns * 112     ns *  8 ascii : ns * physical maximum (e.g. 500 or 40)
 * ns * 0x78    ns * 120     ns *  8 ascii : ns * digital minimum (e.g. -2048)
 * ns * 0x80    ns * 128     ns *  8 ascii : ns * digital maximum (e.g. 2047)
 * ns * 0x88    ns * 136     ns * 80 ascii : ns * prefiltering (e.g. HP:0.1Hz LP:75Hz N:60)
 * ns * 0xD8    ns * 216     ns *  8 ascii : ns * nr of samples in each data record
 * ns * 0xE0    ns * 224     ns * 32 ascii : ns * reserved
 *</pre>
 *<br>
 * ns: number of signals<br>
 *<br>
 * All fields are left aligned and filled up with spaces, no NULL's.<br>
 *<br>
 * Only printable ASCII characters are allowed.<br>
 *<br>
 * Decimal separator (if any) must be a dot. No grouping characters in numbers.<br>
 *<br>
 *<br>
 * For more info about the EDF and EDF+ format, visit: <a href="https://edfplus.info/specs/">https://edfplus.info/specs/</a><br>
 *<br>
 * For more info about the BDF and BDF+ format, visit: <a href="https://www.teuniz.net/edfbrowser/bdfplus%20format%20description.html">https://www.teuniz.net/edfbrowser/bdfplus%20format%20description.html</a><br>
 *<br>
 *<br>
 * note: In EDF, the sensitivity (e.g. uV/bit) and offset are stored using four parameters:<br>
 * digital maximum and minimum, and physical maximum and minimum.<br>
 * Here, digital means the raw data coming from a sensor or ADC. Physical means the units like uV.<br>
 * The sensitivity in units/bit is calculated as follows:<br>
 *<br>
 * units per bit = (physical max - physical min) / (digital max - digital min)<br>
 *<br>
 * The digital offset is calculated as follows:<br>
 *<br>
 * offset = (physical max / units per bit) - digital max<br>
 *<br>
 * For a better explanation about the relation between digital data and physical data,<br>
 * read the document "Coding Schemes Used with Data Converters" (PDF):<br>
 *<br>
 * <a href="https://www.ti.com/general/docs/lit/getliterature.tsp?baseLiteratureNumber=sbaa042">https://www.ti.com/general/docs/lit/getliterature.tsp?baseLiteratureNumber=sbaa042</a><br>
 *<br>
 * note: An EDF file usually contains multiple so-called datarecords. One datarecord usually has a duration of one second (this is the default but it is not mandatory!).<br>
 * In that case a file with a duration of five minutes contains 300 datarecords. The duration of a datarecord can be freely choosen but, if possible, use values from<br>
 * 0.1 to 1 second for easier handling. Just make sure that the total size of one datarecord, expressed in bytes, does not exceed 10MByte (15MBytes for BDF(+)).<br>
 *<br>
 * The RECOMMENDATION of a maximum datarecordsize of 61440 bytes in the EDF and EDF+ specification was usefull in the time people were still using DOS as their main operating system.<br>
 * Using DOS and fast (near) pointers (16-bit pointers), the maximum allocatable block of memory was 64KByte.<br>
 * This is not a concern anymore so the maximum datarecord size now is limited to 10MByte for EDF(+) and 15MByte for BDF(+). This helps to accommodate for higher samplingrates<br>
 * used by modern Analog to Digital Converters.<br>
 *<br>
 * EDF header character encoding: The EDF specification says that only (printable) ASCII characters are allowed.<br>
 * When writing the header info, EDFlib will assume you are using Latin1 encoding and it will automatically convert<br>
 * characters with accents, umlauts, tilde, etc. to their "normal" equivalent without the accent/umlaut/tilde/etc.<br>
 * in order to create a valid EDF file.<br>
 *<br>
 * The description/name of an EDF+ annotation on the other hand, is encoded in UTF-8.<br>
 *<br>
 *
 * @author Teunis van Beelen
 */
public class EDFreader
{

public static final long EDFLIB_TIME_DIMENSION     = 10000000L;
public static final int  EDFLIB_MAXSIGNALS               = 640;
public static final int  EDFLIB_MAX_ANNOTATION_LEN       = 512;

public static final int EDFSEEK_SET = 0;
public static final int EDFSEEK_CUR = 1;
public static final int EDFSEEK_END = 2;


/* the following defines are used in the member "filetype" of the edf_hdr_struct */
/* and as return value for the function edfopen_file_readonly() */
public static final int EDFLIB_FILETYPE_EDF                 = 0;
public static final int EDFLIB_FILETYPE_EDFPLUS             = 1;
public static final int EDFLIB_FILETYPE_BDF                 = 2;
public static final int EDFLIB_FILETYPE_BDFPLUS             = 3;
public static final int EDFLIB_MALLOC_ERROR                = -1;
public static final int EDFLIB_NO_SUCH_FILE_OR_DIRECTORY   = -2;

/* when this error occurs, try to open the file with EDFbrowser,
   it will give you full details about the cause of the error. */
public static final int EDFLIB_FILE_CONTAINS_FORMAT_ERRORS = -3;

public static final int EDFLIB_MAXFILES_REACHED            = -4;
public static final int EDFLIB_FILE_READ_ERROR             = -5;
public static final int EDFLIB_FILE_ALREADY_OPENED         = -6;
public static final int EDFLIB_FILETYPE_ERROR              = -7;
public static final int EDFLIB_FILE_WRITE_ERROR            = -8;
public static final int EDFLIB_NUMBER_OF_SIGNALS_INVALID   = -9;
public static final int EDFLIB_FILE_IS_DISCONTINUOUS      = -10;
public static final int EDFLIB_INVALID_READ_ANNOTS_VALUE  = -11;
public static final int EDFLIB_INVALID_ARGUMENT           = -12;
public static final int EDFLIB_FILE_CLOSED                = -13;

/* values for annotations */
public static final int EDFLIB_DO_NOT_READ_ANNOTATIONS = 0;
public static final int EDFLIB_READ_ANNOTATIONS        = 1;
public static final int EDFLIB_READ_ALL_ANNOTATIONS    = 2;

/* the following defines are possible errors returned by the first sample write action */
public static final int EDFLIB_NO_SIGNALS                 = -20;
public static final int EDFLIB_TOO_MANY_SIGNALS           = -21;
public static final int EDFLIB_NO_SAMPLES_IN_RECORD       = -22;
public static final int EDFLIB_DIGMIN_IS_DIGMAX           = -23;
public static final int EDFLIB_DIGMAX_LOWER_THAN_DIGMIN   = -24;
public static final int EDFLIB_PHYSMIN_IS_PHYSMAX         = -25;
public static final int EDFLIB_DATARECORD_SIZE_TOO_BIG    = -26;


private final int EDFLIB_VERSION = 102;

/* max size of annotationtext */
private final int EDFLIB_WRITE_MAX_ANNOTATION_LEN = 40;

/* bytes in datarecord for EDF annotations, must be an integer multiple of three and two */
private final int EDFLIB_ANNOTATION_BYTES = 114;

/* for writing only */
private final int EDFLIB_MAX_ANNOTATION_CHANNELS = 64;

private final int EDFLIB_ANNOT_MEMBLOCKSZ = 1000;

private String[] param_label;
private String[] param_transducer;
private String[] param_physdimension;
private double[] param_phys_min;
private double[] param_phys_max;
private int[]    param_dig_min;
private int[]    param_dig_max;
private String[] param_prefilter;
private int[]    param_smp_per_record;
private String[] param_reserved;
private double[] param_offset;
private int[]    param_buf_offset;
private double[] param_bitvalue;
private int[]    param_annotation;
private long[]   param_sample_pntr;

private String path;
private int    filetype;
private String version;
private String patient;
private String recording;
private String plus_patientcode;
private String plus_gender;
private String plus_birthdate;
private String plus_patient_name;
private String plus_patient_additional;
private String plus_startdate;
private String plus_admincode;
private String plus_technician;
private String plus_equipment;
private String plus_recording_additional;
private long   l_starttime;
private int    startdate_day;
private int    startdate_month;
private int    startdate_year;
private int    starttime_second;
private int    starttime_minute;
private int    starttime_hour;
private String reserved;
private int    hdrsize;
private int    edfsignals;
private long   datarecords;
private int    recordsize;
private int[]  annot_ch;
private int    nr_annot_chns;
private int[]  mapped_signals;
private int    edf;
private int    edfplus;
private int    bdf;
private int    bdfplus;
private int    discontinuous;
private int    signal_write_sequence_pos;
private long   starttime_offset;
private double data_record_duration;
private long   long_data_record_duration;
private int    annots_in_file;
private int    annotlist_sz;
private int    total_annot_bytes;
private int    eq_sf;
private int status_ok;

private RandomAccessFile file_in;

private byte[] hdr;

/**
 * This list contains the annotations (if any).
 */
public ArrayList<EDFAnnotationStruct> annotationslist;


/**
 * Creates an EDFreader object that reads from an EDF(+)/BDF(+) file. <br>
 *<br>
 * Notes:<br>
 *<br>
 * Annotationsignals<br>
 * =================<br>
 *<br>
 * EDFplus and BDFplus store the annotations in one or more signals (in order to be backwards compatibel with EDF and BDF).<br>
 * The counting of the signals in the file starts at 0. Signals used for annotations are skipped by EDFlib.<br>
 * This means that the annotationsignal(s) in the file are hided.<br>
 * Use the function get_annotation() to get the annotations.<br>
 *<br>
 * So, when a file contains 5 signals and the third signal is used to store the annotations, the library will<br>
 * report that there are only 4 signals in the file.<br>
 * The library will "map" the signalnumbers as follows: 0->0, 1->1, 2->3, 3->4.<br>
 * This way you don't need to worry about which signals are annotationsignals, the library will take care of it.<br>
 *<br>
 * How the library stores time-values<br>
 * ==================================<br>
 *<br>
 * To avoid rounding errors, the library stores some timevalues in variables of type long.<br>
 * In order not to loose the subsecond precision, all timevalues have been multiplied by 10000000.<br>
 * This will limit the timeresolution to 100 nanoSeconds. To calculate the amount of seconds, divide<br>
 * the timevalue by 10000000 or use the macro EDFLIB_TIME_DIMENSION.<br>
 *
 * @param  p_path  The path to the file.
 *
 * @throws IOException, EDFException
 */
public EDFreader(String p_path) throws IOException, EDFException
{
  int i, err;

  annotationslist = new ArrayList<EDFAnnotationStruct>(0);

  path = p_path;

  file_in = new RandomAccessFile(path, "r");

  err = checkEDFheader();
  if(err != 0)
  {
    file_in.close();

    throw new EDFException(EDFLIB_FILE_CONTAINS_FORMAT_ERRORS, "File is not valid EDF(+) or BDF(+).\n");
  }

  if(discontinuous != 0)
  {
    file_in.close();

    throw new EDFException(EDFLIB_FILE_IS_DISCONTINUOUS, "Library does not support discontinuous EDF files.\n");
  }

  annotlist_sz = 0;

  annots_in_file = 0;

  if(((edfplus == 0)) && (bdfplus == 0))
  {
    plus_patientcode = "";
    plus_gender = "";
    plus_birthdate = "";
    plus_patient_name = "";
    plus_patient_additional = "";
    plus_admincode = "";
    plus_technician = "";
    plus_equipment = "";
    plus_recording_additional = "";
  }
  else
  {
    patient = "";
    recording = "";

    err = get_annotations(EDFLIB_READ_ALL_ANNOTATIONS);
    if(err != 0)
    {
      file_in.close();

      throw new EDFException(EDFLIB_FILE_CONTAINS_FORMAT_ERRORS, "File is not valid EDF(+) or BDF(+).\n");
    }
  }

  status_ok = 1;
}


/**
 * If version is "1.00" than it will return 100. <br>
 *
 * @return version number of this library, multiplied by hundred.
 */
public int version()
{
  return EDFLIB_VERSION;
}

/**
 * Gets the file type. <br>
 * File type can be one of the following:<br>
 * EDFLIB_FILETYPE_EDF      = 0<br>
 * EDFLIB_FILETYPE_EDFPLUS  = 1<br>
 * EDFLIB_FILETYPE_BDF      = 2<br>
 * EDFLIB_FILETYPE_BDFPLUS  = 3<br>
 *
 * @return EDF file type
 */
public int getFileType()
{
  return filetype;
}

/**
 * Returns the hours of the starttime of the file. <br>
 */
public int getStartTimeHour()
{
  return starttime_hour;
}

/**
 * Returns the minutes of the starttime of the file. <br>
 */
public int getStartTimeMinute()
{
  return starttime_minute;
}

/**
 * Returns the seconds of the starttime of the file. <br>
 */
public int getStartTimeSecond()
{
  return starttime_second;
}

/**
 * Returns the sub-second part of the starttime of the file. <br>
 * The returned value is always less than one second.<br>
 *
 * @return sub-second part of the start time expressed in units of 100 nanoSeconds
 */
public long getStartTimeSubSecond()
{
  return starttime_offset;
}

/**
 * Returns the days of the startdate of the file. <br>
 *
 * @return 1 - 31
 */
public int getStartDateDay()
{
  return startdate_day;
}

/**
 * Returns the months of the startdate of the file. <br>
 *
 * @return 1 - 12
 */
public int getStartDateMonth()
{
  return startdate_month;
}

/**
 * Returns the years of the startdate of the file. <br>
 *
 * @return year e.g. 2017
 */
public int getStartDateYear()
{
  return startdate_year;
}

/**
 * Returns the patient field of the file header. <br>
 * Is always empty if filetype is EDF+ or BDF+.<br>
 */
public String getPatient()
{
  return patient;
}

/**
 * Returns the patient code. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getPatientCode()
{
  return plus_patientcode;
}

/**
 * Returns the gender. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getPatientGender()
{
  return plus_gender;
}

/**
 * Returns the birthdate. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getPatientBirthDate()
{
  return plus_birthdate;
}

/**
 * Returns the patient name. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getPatientName()
{
  return plus_patient_name;
}

/**
 * Returns the additional info (if any) of the patient field of the file header. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getPatientAdditional()
{
  return plus_patient_additional;
}

/**
 * Returns the recording field of the file header. <br>
 * Is always empty if filetype is EDF+ or BDF+.<br>
 */
public String getRecording()
{
  return recording;
}

/**
 * Returns the administration code. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getAdministrationCode()
{
  return plus_admincode;
}

/**
 * Returns the technician who performed the recording. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getTechnician()
{
  return plus_technician;
}

/**
 * Returns the equipment used for the recording. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getEquipment()
{
  return plus_equipment;
}

/**
 * Returns the additional info (if any) of the recording field of the file header. <br>
 * Is always empty if filetype is EDF or BDF.<br>
 */
public String getRecordingAdditional()
{
  return plus_recording_additional;
}

/**
 * Returns the reserved field of the file header. <br>
 *
 * @return reserved field
 */
public String getReserved()
{
  return reserved;
}

/**
 * Returns the number of signals in the file. <br>
 *
 * @return number of signals
 */
public int getNumSignals()
{
  return (edfsignals - nr_annot_chns);
}

/**
 * Returns the number of datarecords in the file. <br>
 * The duration of a file = number of datarecords * datarecord duration<br>
 *
 * @return number of datarecords
 */
public long getNumDataRecords()
{
  return datarecords;
}

/**
 * Returns the duration of a datarecord. <br>
 * The duration of a file = number of datarecords * datarecord duration<br>
 *
 * @return datarecord duration expressed in units of 100 nanoSeconds
 */
public long getLongDataRecordDuration()
{
  return long_data_record_duration;
}

/**
 * Returns the duration of the file (recording time). <br>
 *
 * @return file duration expressed in units of 100 nanoSeconds
 */
public long getFileDuration()
{
  return (long_data_record_duration * datarecords);
}

/**
 * Returns the number of samples of a signal in a datarecord. <br>
 * This number equals the samplerate only if the datarecord duration equals one second.<br>
 * Samplerate can be calculated as follows: fs = samples per record / datarecord duration<br>
 * The total number of samples of a signal in a file = samples per record * datarecords<br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return number of samples in a datarecord
 */
public int getSampelsPerDataRecord(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_smp_per_record[mapped_signals[s]];
}

/**
 * Returns the total number of samples of a signal in the recording. <br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return number of samples in the recording
 */
public long getTotalSamples(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return ((long)param_smp_per_record[mapped_signals[s]] * datarecords);
}

/**
 * Returns the samplefrequency of a signal. <br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return samplefrequency
 */
public double getSampleFrequency(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return ((double)param_smp_per_record[mapped_signals[s]] / ((double)long_data_record_duration / (double)EDFLIB_TIME_DIMENSION));
}

/**
 * Returns the label (name) of a signal. <br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return label
 */
public String getSignalLabel(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_label[mapped_signals[s]];
}

/**
 * Returns the transducer (or electrode type) used for a signal. <br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return transducer
 */
public String getTransducer(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_transducer[mapped_signals[s]];
}

/**
 * Returns the physical dimension (unit) of a signal. <br>
 * e.g. "uV", "mmHg", "mV", etc.<br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return physical dimension
 */
public String getPhysicalDimension(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_physdimension[mapped_signals[s]];
}

/**
 * Returns the physical minimum of a signal. <br>
 * note: In EDF, the sensitivity (e.g. uV/bit) and offset are stored using four parameters:<br>
 * digital maximum and minimum, and physical maximum and minimum.<br>
 * Here, digital means the raw data coming from a sensor or ADC. Physical means the units like uV.<br>
 * The sensitivity in units/bit is calculated as follows:<br>
 *<br>
 * units per bit = (physical max - physical min) / (digital max - digital min)<br>
 *<br>
 * The digital offset is calculated as follows:<br>
 *<br>
 * offset = (physical max / units per bit) - digital max<br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return physical minimum
 */
public double getPhysicalMinimum(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_phys_min[mapped_signals[s]];
}

/**
 * Returns the physical maximum of a signal. <br>
 * note: In EDF, the sensitivity (e.g. uV/bit) and offset are stored using four parameters:<br>
 * digital maximum and minimum, and physical maximum and minimum.<br>
 * Here, digital means the raw data coming from a sensor or ADC. Physical means the units like uV.<br>
 * The sensitivity in units/bit is calculated as follows:<br>
 *<br>
 * units per bit = (physical max - physical min) / (digital max - digital min)<br>
 *<br>
 * The digital offset is calculated as follows:<br>
 *<br>
 * offset = (physical max / units per bit) - digital max<br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return physical maximum
 */
public double getPhysicalMaximum(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_phys_max[mapped_signals[s]];
}

/**
 * Returns the digital minimum of a signal. <br>
 * note: In EDF, the sensitivity (e.g. uV/bit) and offset are stored using four parameters:<br>
 * digital maximum and minimum, and physical maximum and minimum.<br>
 * Here, digital means the raw data coming from a sensor or ADC. Physical means the units like uV.<br>
 * The sensitivity in units/bit is calculated as follows:<br>
 *<br>
 * units per bit = (physical max - physical min) / (digital max - digital min)<br>
 *<br>
 * The digital offset is calculated as follows:<br>
 *<br>
 * offset = (physical max / units per bit) - digital max<br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return digital minimum
 */
public int getDigitalMinimum(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_dig_min[mapped_signals[s]];
}

/**
 * Returns the digital maximum of a signal. <br>
 * note: In EDF, the sensitivity (e.g. uV/bit) and offset are stored using four parameters:<br>
 * digital maximum and minimum, and physical maximum and minimum.<br>
 * Here, digital means the raw data coming from a sensor or ADC. Physical means the units like uV.<br>
 * The sensitivity in units/bit is calculated as follows:<br>
 *<br>
 * units per bit = (physical max - physical min) / (digital max - digital min)<br>
 *<br>
 * The digital offset is calculated as follows:<br>
 *<br>
 * offset = (physical max / units per bit) - digital max<br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return digital maximum
 */
public int getDigitalMaximum(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_dig_max[mapped_signals[s]];
}

/**
 * Returns the pre-filtering of a signal. <br>
 * E.g.: "LPF:250Hz", "LPF:0.05Hz", "N:60Hz"
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return pre-filter
 */
public String getPreFilter(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_prefilter[mapped_signals[s]];
}

/**
 * Returns the reserved field in the header of a signal. <br>
 *
 * @param s signal number, zero-based
 *
 * @throws EDFException
 *
 * @return reserved field
 */
public String getReserved(int s) throws EDFException
{
  if((s < 0) || (s >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_reserved[mapped_signals[s]];
}

/**
 * Read digital samples. <br>
 * Reads buf.length samples from edfsignal, starting from the current sample position indicator, into buf.<br>
 * The values are the "raw" digital values as stored in the EDF file.<br>
 * (No conversion is done to their physical values.)<br>
 * The sample position indicator will be increased with the number of samples read.<br>
 *
 * @param edfsignal signal number, zero-based
 *
 * @return number of samples read into the buffer (this can be less than buf.length or zero!)
 *
 * @throws IOException, EDFException
 */
public int readDigitalSamples(int edfsignal, int[] buf) throws IOException, EDFException
{
  int i, n,
      channel;

  long smp_in_file,
       offset,
       sample_pntr,
       smp_per_record;

  if(status_ok == 0)
  {
    throw new EDFException(EDFLIB_FILE_CLOSED, "File is closed.\n");
  }

  if((edfsignal < 0) || (edfsignal >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  n = buf.length;
  if(n < 1)
  {
    throw new EDFException(EDFLIB_INVALID_ARGUMENT, "Invalid buffer length.\n");
  }

  channel = mapped_signals[edfsignal];

  smp_in_file = param_smp_per_record[channel] * datarecords;

  if((param_sample_pntr[channel] + n) > smp_in_file)
  {
    n = (int)(smp_in_file - param_sample_pntr[channel]);

    if(n == 0)
    {
      return 0;
    }

    if(n < 0)
    {
      return -1;
    }
  }

  offset = hdrsize;
  offset += (param_sample_pntr[channel] / param_smp_per_record[channel]) * recordsize;
  offset += param_buf_offset[channel];
  if(edf != 0)
  {
    file_in.seek(offset + ((param_sample_pntr[channel] % param_smp_per_record[channel]) * 2));
  }
  else
  {
    file_in.seek(offset + ((param_sample_pntr[channel] % param_smp_per_record[channel]) * 3));
  }

  sample_pntr = param_sample_pntr[channel];

  smp_per_record = param_smp_per_record[channel];

  for(i=0; i<n; i++)
  {
    if((sample_pntr % smp_per_record) == 0)
    {
      if(i != 0)
      {
        offset += recordsize;

        file_in.seek(offset);
      }
    }

    if(edf != 0)
    {
      buf[i] = file_in.readUnsignedByte();

      buf[i] |= file_in.readByte() << 8;
    }
    else
    {
      buf[i] = file_in.readUnsignedByte();

      buf[i] |= file_in.readUnsignedByte() << 8;

      buf[i] |= file_in.readByte() << 16;
    }

    sample_pntr++;
  }

  param_sample_pntr[channel] = sample_pntr;

  return n;
}

/**
 * Read physical samples. <br>
 * Reads buf.length samples from edfsignal, starting from the current sample position indicator, into buf.<br>
 * The values are converted to their physical values e.g. microVolts, beats per minute, mmHg, etc.<br>
 * The sample position indicator will be increased with the number of samples read.<br>
 * Returns the number of samples read (this can be less than buf.length or zero!).<br>
 *
 * @param edfsignal signal number, zero-based
 *
 * @return number of samples read into the buffer (this can be less than buf.length or zero!)
 *
 * @throws IOException, EDFException
 */
public int readPhysicalSamples(int edfsignal, double[] buf) throws IOException, EDFException
{
  int i, n, tmp,
      channel;

  long smp_in_file,
       offset,
       sample_pntr,
       smp_per_record;

  if(status_ok == 0)
  {
    throw new EDFException(EDFLIB_FILE_CLOSED, "File is closed.\n");
  }

  if((edfsignal < 0) || (edfsignal >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  n = buf.length;
  if(n < 1)
  {
    throw new EDFException(EDFLIB_INVALID_ARGUMENT, "Invalid buffer length.\n");
  }

  channel = mapped_signals[edfsignal];

  smp_in_file = param_smp_per_record[channel] * datarecords;

  if((param_sample_pntr[channel] + n) > smp_in_file)
  {
    n = (int)(smp_in_file - param_sample_pntr[channel]);

    if(n == 0)
    {
      return 0;
    }

    if(n < 0)
    {
      return -1;
    }
  }

  offset = hdrsize;
  offset += (param_sample_pntr[channel] / param_smp_per_record[channel]) * recordsize;
  offset += param_buf_offset[channel];
  if(edf != 0)
  {
    file_in.seek(offset + ((param_sample_pntr[channel] % param_smp_per_record[channel]) * 2));
  }
  else
  {
    file_in.seek(offset + ((param_sample_pntr[channel] % param_smp_per_record[channel]) * 3));
  }

  sample_pntr = param_sample_pntr[channel];

  smp_per_record = param_smp_per_record[channel];

  for(i=0; i<n; i++)
  {
    if((sample_pntr % smp_per_record) == 0)
    {
      if(i != 0)
      {
        offset += recordsize;

        file_in.seek(offset);
      }
    }

    if(edf != 0)
    {
      tmp = file_in.readUnsignedByte();

      tmp |= file_in.readByte() << 8;
    }
    else
    {
      tmp = file_in.readUnsignedByte();

      tmp |= file_in.readUnsignedByte() << 8;

      tmp |= file_in.readByte() << 16;
    }

    buf[i] = param_bitvalue[channel] * (param_offset[channel] + (double)tmp);

    sample_pntr++;
  }

  param_sample_pntr[channel] = sample_pntr;

  return n;
}

/**
 * Returns the file position. <br>
 * The ftell() function obtains the current value of the sample position indicator for the edfsignal pointed to by edfsignal.<br>
 * Returns the current offset.<br>
 * Note that every signal has it's own independent sample position indicator and ftell() affects only one of them.<br>
 *
 * @param edfsignal signal number, zero-based
 *
 * @return sample position relative to start of file
 *
 * @throws EDFException
 */
public long ftell(int edfsignal) throws EDFException
{
  if(status_ok == 0)
  {
    throw new EDFException(EDFLIB_FILE_CLOSED, "File is closed.\n");
  }

  if((edfsignal < 0) || (edfsignal >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  return param_sample_pntr[mapped_signals[edfsignal]];
}

/**
 * Rewinds the sample position to zero. <br>
 * The rewind() function sets the sample position indicator for the edfsignal pointed to by edfsignal to the beginning of the file.<br>
 * It is equivalent to: (void) fseek(int edfsignal, 0L, EDFSEEK_SET)<br>
 * Note that every signal has it's own independent sample position indicator and rewind() affects only one of them.<br>
 *
 * @param edfsignal signal number, zero-based
 *
 * @throws EDFException
 */
public void rewind(int edfsignal) throws EDFException
{
  if(status_ok == 0)
  {
    throw new EDFException(EDFLIB_FILE_CLOSED, "File is closed.\n");
  }

  if((edfsignal < 0) || (edfsignal >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  param_sample_pntr[mapped_signals[edfsignal]] = 0L;
}

/**
 * Sets the file offset / sample position. <br>
 * The fseek() function sets the sample position indicator for the edfsignal pointed to by edfsignal.<br>
 * The new position, measured in samples, is obtained by adding offset samples to the position specified by whence.<br>
 * If whence is set to EDFSEEK_SET, EDFSEEK_CUR, or EDFSEEK_END, the offset is relative to the start of the file,<br>
 * the current position indicator, or end-of-file, respectively.<br>
 * Returns the current offset.<br>
 * Note that every signal has it's own independent sample position indicator and fseek() affects only one of them.<br>
 *
 * @param edfsignal signal number, zero-based
 *
 * @param  offset new position
 *
 * @param whence EDFSEEK_SET, EDFSEEK_CUR, or EDFSEEK_END
 *
 * @return the new sample position relative to the start of the file
 *
 * @throws EDFException
 */
public long fseek(int edfsignal, long offset, int whence) throws EDFException
{
  long smp_in_file;

  int channel;

  if(status_ok == 0)
  {
    throw new EDFException(EDFLIB_FILE_CLOSED, "File is closed.\n");
  }

  if((edfsignal < 0) || (edfsignal >= (edfsignals - nr_annot_chns)))
  {
    throw new EDFException(EDFLIB_NUMBER_OF_SIGNALS_INVALID, "Invalid signal number.\n");
  }

  channel = mapped_signals[edfsignal];

  smp_in_file = param_smp_per_record[channel] * datarecords;

  if(whence == EDFSEEK_SET)
  {
    param_sample_pntr[channel] = offset;
  }

  if(whence == EDFSEEK_CUR)
  {
    param_sample_pntr[channel] += offset;
  }

  if(whence == EDFSEEK_END)
  {
    param_sample_pntr[channel] = (param_smp_per_record[channel] * datarecords) + offset;
  }

  if(param_sample_pntr[channel] > smp_in_file)
  {
    param_sample_pntr[channel] = smp_in_file;
  }

  if(param_sample_pntr[channel] < 0L)
  {
    param_sample_pntr[channel] = 0L;
  }

  return param_sample_pntr[channel];
}

/**
 * Closes the file. <br>
 *
 * @throws IOException
 *
 * @return 0 on success, otherwise -1
 */
public int close() throws IOException
{
  if(status_ok == 0)  return -1;

  file_in.close();

  status_ok = 0;

  return 0;
}

private int checkEDFheader()
{
  int i, j, n, p, r, error, dotposition;

  byte[] str4 = new byte[4];
  byte[] str8 = new byte[8];
  byte[] str16 = new byte[16];
  byte[] str80 = new byte[80];
  byte[] str128 = new byte[128];

  String tmp_str;

/******** Check the base header (first 256 bytes) ********/
  hdr = new byte[256];

  try
  {
    if(file_in.length() < 512)  /* There must be at least one signal thus the header must have at least 512 bytes */
    {
      return -1;
    }

    file_in.seek(0);

    file_in.read(hdr);
  }
  catch(IOException e)
  {
    return -1;
  }

/******** Check for non-printable ASCII characters ********/
  for(i=1; i<256; i++)
  {
    if((hdr[i] < 32) || (hdr[i] > 126))
    {
      return -1;
    }
  }

/**************************** VERSION ***************************************/
  if(hdr[0] == '0')
  {
    for(i=1; i<8; i++)
    {
      if(hdr[i] != ' ')
      {
        return -1;
      }
    }

    edf = 1;
  }
  else if((hdr[0] == -1) &&
          (hdr[1] == 'B') &&
          (hdr[2] == 'I') &&
          (hdr[3] == 'O') &&
          (hdr[4] == 'S') &&
          (hdr[5] == 'E') &&
          (hdr[6] == 'M') &&
          (hdr[7] == 'I'))
      {
        bdf = 1;
      }
      else
      {
        return -1;
      }

/********************* PATIENTNAME *********************************************/

  try
  {
    patient = new String(hdr, 8, 80, "US-ASCII");
  }
  catch(UnsupportedEncodingException e)
  {
    return -1;
  }

/********************* RECORDING *********************************************/

  try
  {
    recording = new String(hdr, 88, 80, "US-ASCII");
  }
  catch(UnsupportedEncodingException e)
  {
    return -1;
  }

/********************* STARTDATE *********************************************/

  error = 0;
  if((hdr[170]!='.')||(hdr[173]!='.'))  error = 1;
  if((hdr[168]<48)||(hdr[168]>57))      error = 1;
  if((hdr[169]<48)||(hdr[169]>57))      error = 1;
  if((hdr[171]<48)||(hdr[171]>57))      error = 1;
  if((hdr[172]<48)||(hdr[172]>57))      error = 1;
  if((hdr[174]<48)||(hdr[174]>57))      error = 1;
  if((hdr[175]<48)||(hdr[175]>57))      error = 1;
  if(error != 0)
  {
    return -1;
  }

  for(i=0; i<8; i++)
  {
    str8[i] = hdr[168+i];
  }

  str8[2] = 0;
  str8[5] = 0;

  startdate_day = atoi_nonlocalized(str8, 0);
  startdate_month = atoi_nonlocalized(str8, 3);
  startdate_year = atoi_nonlocalized(str8, 6);

  if((startdate_day < 1) || (startdate_day > 31))
  {
    return -1;
  }

  if((startdate_month < 1) || (startdate_month > 12))
  {
    return -1;
  }

  if(startdate_year>84)
  {
    startdate_year += 1900;
  }
  else
  {
    startdate_year += 2000;
  }

/********************* STARTTIME *********************************************/

  error = 0;
  if((hdr[178]!='.')||(hdr[181]!='.'))  error = 1;
  if((hdr[176]<48)||(hdr[176]>57))      error = 1;
  if((hdr[177]<48)||(hdr[177]>57))      error = 1;
  if((hdr[179]<48)||(hdr[179]>57))      error = 1;
  if((hdr[180]<48)||(hdr[180]>57))      error = 1;
  if((hdr[182]<48)||(hdr[182]>57))      error = 1;
  if((hdr[183]<48)||(hdr[183]>57))      error = 1;
  if(error != 0)
  {
    return -1;
  }

  for(i=0; i<8; i++)
  {
    str8[i] = hdr[176+i];
  }

  str8[2] = 0;
  str8[5] = 0;

  starttime_hour = atoi_nonlocalized(str8, 0);
  starttime_minute = atoi_nonlocalized(str8, 3);
  starttime_second = atoi_nonlocalized(str8, 6);

  if((starttime_hour   > 23) ||
     (starttime_minute > 59) ||
     (starttime_second > 59))
  {
    return -1;
  }

  l_starttime = 3600 * starttime_hour;
  l_starttime += 60 * starttime_minute;
  l_starttime += starttime_second;
  l_starttime *= EDFLIB_TIME_DIMENSION;

/***************** NUMBER OF SIGNALS IN HEADER *******************************/
  for(i=0; i<4; i++)
  {
    str4[i] = hdr[252+i];
  }

  if(is_integer_number(str4) != 0)  return -1;

  edfsignals = atoi_nonlocalized(str4, 0);

  if((edfsignals < 1) || (edfsignals > EDFLIB_MAXSIGNALS))  return -1;

  annot_ch = new int[edfsignals];
  mapped_signals = new int[edfsignals];

/***************** NUMBER OF BYTES IN HEADER *******************************/
  for(i=0; i<8; i++)
  {
    str8[i] = hdr[184+i];
  }

  if(is_integer_number(str8) != 0)  return -1;

  hdrsize = atoi_nonlocalized(str8, 0);

  if(hdrsize != ((edfsignals + 1) * 256))  return -1;

/********************* RESERVED FIELD *************************************/

  try
  {
    reserved = new String(hdr, 192, 44, "US-ASCII");
  }
  catch(UnsupportedEncodingException e)
  {
    return -1;
  }

  if(edf != 0)
  {
    filetype = EDFLIB_FILETYPE_EDF;

    if(reserved.substring(0, 5).equals("EDF+C"))
    {
      edfplus = 1;
      filetype = EDFLIB_FILETYPE_EDFPLUS;
    }

    if(reserved.substring(0, 5).equals("EDF+D"))
    {
      edfplus = 1;
      discontinuous = 1;
      filetype = EDFLIB_FILETYPE_EDFPLUS;
    }
  }

  if(bdf != 0)
  {
    filetype = EDFLIB_FILETYPE_BDF;

    if(reserved.substring(0, 5).equals("BDF+C"))
    {
      bdfplus = 1;
      filetype = EDFLIB_FILETYPE_BDFPLUS;
    }

    if(reserved.substring(0, 5).equals("BDF+D"))
    {
      bdfplus = 1;
      discontinuous = 1;
      filetype = EDFLIB_FILETYPE_BDFPLUS;
    }
  }

/********************* NUMBER OF DATARECORDS *************************************/
  for(i=0; i<8; i++)
  {
    str8[i] = hdr[236+i];
  }

  if(is_integer_number(str8) != 0)  return -1;

  datarecords = atoi_nonlocalized(str8, 0);

  if(datarecords < 1)  return -1;

/********************* DATARECORD DURATION *************************************/
  for(i=0; i<8; i++)
  {
    str8[i] = hdr[244+i];
  }

  if(is_number(str8) != 0)  return -1;

  data_record_duration = atof_nonlocalized(str8);

  if(data_record_duration < -0.000001)  return -1;

  long_data_record_duration = get_long_duration_hdr(str8);

/********************* START WITH THE SIGNALS IN THE HEADER *********************/

  hdr = new byte[hdrsize];

  try
  {
    if(file_in.length() < hdrsize)
    {
      return -1;
    }

    file_in.seek(0);

    file_in.read(hdr);
  }
  catch(IOException e)
  {
    return -1;
  }

/******** Check for non-printable ASCII characters ********/

  for(i=1; i<hdrsize; i++)
  {
    if((hdr[i] < 32) || (hdr[i] > 126))
    {
      return -1;
    }
  }

/**************************** LABELS *************************************/

  param_label = new String[edfsignals];

  param_annotation = new int[edfsignals];

  nr_annot_chns = 0;
  for(i=0; i<edfsignals; i++)
  {
    try
    {
      param_label[i] = tmp_str = new String(hdr, 256 + (i * 16), 16, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }

    if(edfplus != 0)
    {
      if(param_label[i].substring(0, 16).equals("EDF Annotations "))
      {
        annot_ch[nr_annot_chns] = i;
        nr_annot_chns++;
        param_annotation[i] = 1;
      }
    }

    if(bdfplus != 0)
    {
      if(param_label[i].substring(0, 16).equals("BDF Annotations "))
      {
        annot_ch[nr_annot_chns] = i;
        nr_annot_chns++;
        param_annotation[i] = 1;
      }
    }
  }

  if(((edfplus != 0) || (bdfplus != 0)) && (nr_annot_chns == 0))
  {
    return -1;
  }

  if((edfsignals != nr_annot_chns) || ((edfplus == 0) && (bdfplus == 0)))
  {
    if(data_record_duration < 0.0000001)
    {
      return -1;
    }
  }

/**************************** TRANSDUCER TYPES *************************************/

  param_transducer = new String[edfsignals];

  for(i=0; i<edfsignals; i++)
  {
    if((edfplus != 0) || (bdfplus != 0))
    {
      if(param_annotation[i] != 0)
      {
        for(j=0; j<80; j++)
        {
          if(hdr[256 + (edfsignals * 16) + (i * 80) + j] != ' ')
          {
            return -1;
          }
        }
      }
    }

    try
    {
      param_transducer[i] = tmp_str = new String(hdr, 256 + (edfsignals * 16) + (i * 80), 80, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }
  }

/**************************** PHYSICAL DIMENSIONS *************************************/

  param_physdimension = new String[edfsignals];

  for(i=0; i<edfsignals; i++)
  {
    try
    {
      param_physdimension[i] = tmp_str = new String(hdr, 256 + (edfsignals * 96) + (i * 8), 8, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }
  }

/**************************** PHYSICAL MINIMUMS *************************************/

  param_phys_min = new double[edfsignals];

  for(i=0; i<edfsignals; i++)
  {
    for(j=0; j<8; j++)
    {
      str8[j] = hdr[256 + (edfsignals * 104) + (i * 8) + j];
    }

    if(is_number(str8) != 0)  return -1;

    param_phys_min[i] = atof_nonlocalized(str8);
  }

/**************************** PHYSICAL MAXIMUMS *************************************/

  param_phys_max = new double[edfsignals];

  for(i=0; i<edfsignals; i++)
  {
    for(j=0; j<8; j++)
    {
      str8[j] = hdr[256 + (edfsignals * 112) + (i * 8) + j];
    }

    if(is_number(str8) != 0)  return -1;

    param_phys_max[i] = atof_nonlocalized(str8);

    if(param_phys_max[i] == param_phys_min[i])
    {
      return -1;
    }
  }

/**************************** DIGITAL MINIMUMS *************************************/

  param_dig_min = new int[edfsignals];

  for(i=0; i<edfsignals; i++)
  {
    for(j=0; j<8; j++)
    {
      str8[j] = hdr[256 + (edfsignals * 120) + (i * 8) + j];
    }

    if(is_integer_number(str8) != 0)  return -1;

    param_dig_min[i] = atoi_nonlocalized(str8, 0);

    if(edfplus != 0)
    {
      if(param_annotation[i] != 0)
      {
        if(param_dig_min[i] != -32768)
        {
          return -1;
        }
      }
    }

    if(bdfplus != 0)
    {
      if(param_annotation[i] != 0)
      {
        if(param_dig_min[i] != -8388608)
        {
          return -1;
        }
      }
    }

    if(edf != 0)
    {
      if((param_dig_min[i] > 32767) || (param_dig_min[i] < -32768))
      {
        return -1;
      }
    }

    if(bdf != 0)
    {
      if((param_dig_min[i] > 8388607) || (param_dig_min[i] < -8388608))
      {
        return -1;
      }
    }
  }

/**************************** DIGITAL MAXIMUMS *************************************/

  param_dig_max = new int[edfsignals];

  for(i=0; i<edfsignals; i++)
  {
    for(j=0; j<8; j++)
    {
      str8[j] = hdr[256 + (edfsignals * 128) + (i * 8) + j];
    }

    if(is_integer_number(str8) != 0)  return -1;

    param_dig_max[i] = atoi_nonlocalized(str8, 0);

    if(edfplus != 0)
    {
      if(param_annotation[i] != 0)
      {
        if(param_dig_max[i] != 32767)
        {
          return -1;
        }
      }
    }

    if(bdfplus != 0)
    {
      if(param_annotation[i] != 0)
      {
        if(param_dig_max[i] != 8388607)
        {
          return -1;
        }
      }
    }

    if(edf != 0)
    {
      if((param_dig_max[i] > 32767) || (param_dig_max[i] < -32768))
      {
        return -1;
      }
    }

    if(bdf != 0)
    {
      if((param_dig_max[i] > 8388607) || (param_dig_max[i] < -8388608))
      {
        return -1;
      }
    }

    if(param_dig_min[i] >= param_dig_max[i])
    {
      return -1;
    }
  }

/**************************** PREFILTER FIELDS *************************************/

  param_prefilter = new String[edfsignals];

  for(i=0; i<edfsignals; i++)
  {
    if((edfplus != 0) || (bdfplus != 0))
    {
      if(param_annotation[i] != 0)
      {
        for(j=0; j<80; j++)
        {
          if(hdr[256 + (edfsignals * 136) + (i * 80) + j] != ' ')
          {
            return -1;
          }
        }
      }
    }

    try
    {
      param_prefilter[i] = tmp_str = new String(hdr, 256 + (edfsignals * 136) + (i * 80), 80, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }
  }

/*********************** NR OF SAMPLES IN EACH DATARECORD ********************/

  param_smp_per_record = new int[edfsignals];

  recordsize = 0;

  for(i=0; i<edfsignals; i++)
  {
    for(j=0; j<8; j++)
    {
      str8[j] = hdr[256 + (edfsignals * 216) + (i * 8) + j];
    }

    if(is_integer_number(str8) != 0)  return -1;

    param_smp_per_record[i] = atoi_nonlocalized(str8, 0);

    if(param_smp_per_record[i] < 1)
    {
      return -1;
    }

    recordsize += param_smp_per_record[i];
  }

  if(bdf != 0)
  {
    recordsize *= 3;

    if(recordsize > (15 * 1024 * 1024))
    {
      return -1;
    }
  }
  else
  {
    recordsize *= 2;

    if(recordsize > (10 * 1024 * 1024))
    {
      return -1;
    }
  }

/**************************** RESERVED FIELDS *************************************/

  param_reserved = new String[edfsignals];

  for(i=0; i<edfsignals; i++)
  {
    try
    {
      param_reserved[i] = tmp_str = new String(hdr, 256 + (edfsignals * 224) + (i * 32), 32, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }
  }

/********************* EDF+ PATIENTNAME *********************************************/

  if((edfplus != 0) || (bdfplus != 0))
  {
    error = 0;
    dotposition = 0;
    for(i=0; i<80; i++)
    {
      str80[i] = hdr[i + 8];
    }
    for(i=0; i<80; i++)
    {
      if(str80[i] == ' ')
      {
        dotposition = i;
        break;
      }
    }
    dotposition++;
    if((dotposition > 73) || (dotposition < 2))  error = 1;
    if(str80[dotposition + 2] != 'X')
    {
      if(dotposition > 65)  error = 1;
    }
    if((str80[dotposition] != 'M') && (str80[dotposition] != 'F') && (str80[dotposition] != 'X'))  error = 1;
    dotposition++;
    if(str80[dotposition] != ' ')  error = 1;
    if(str80[dotposition + 1] == 'X')
    {
      if(str80[dotposition + 2] != ' ')  error = 1;
      if(str80[dotposition + 3] == ' ')  error = 1;
    }
    else
    {
      if(str80[dotposition + 12] != ' ')  error = 1;
      if(str80[dotposition + 13] == ' ')  error = 1;
      dotposition++;
      for(i=0; i<11; i++)
      {
        str16[i] = str80[dotposition + i];
      }
      str16[11] = 0;
      if((str16[2] != '-') || (str16[6] != '-'))  error = 1;
      str16[2] = 0;
      str16[6] = 0;
      if((str16[0]<48)||(str16[0]>57))  error = 1;
      if((str16[1]<48)||(str16[1]>57))  error = 1;
      if((str16[7]<48)||(str16[7]>57))  error = 1;
      if((str16[8]<48)||(str16[8]>57))  error = 1;
      if((str16[9]<48)||(str16[9]>57))  error = 1;
      if((str16[10]<48)||(str16[10]>57))  error = 1;
      if((atof_nonlocalized(str16) < 1) || (atof_nonlocalized(str16) > 31))  error = 1;
      if((str16[3] != 'J') && (str16[4] != 'A') && (str16[5] != 'N'))
        if((str16[3] != 'F') && (str16[4] != 'E') && (str16[5] != 'B'))
          if((str16[3] != 'M') && (str16[4] != 'A') && (str16[5] != 'R'))
            if((str16[3] != 'A') && (str16[4] != 'P') && (str16[5] != 'R'))
              if((str16[3] != 'M') && (str16[4] != 'A') && (str16[5] != 'Y'))
                if((str16[3] != 'J') && (str16[4] != 'U') && (str16[5] != 'N'))
                  if((str16[3] != 'J') && (str16[4] != 'U') && (str16[5] != 'L'))
                    if((str16[3] != 'A') && (str16[4] != 'U') && (str16[5] != 'G'))
                      if((str16[3] != 'S') && (str16[4] != 'E') && (str16[5] != 'P'))
                        if((str16[3] != 'O') && (str16[4] != 'C') && (str16[5] != 'T'))
                          if((str16[3] != 'N') && (str16[4] != 'O') && (str16[5] != 'V'))
                            if((str16[3] != 'D') && (str16[4] != 'E') && (str16[5] != 'C'))
                              error = 1;
    }
    if(error != 0)
    {
      return -1;
    }

    p = 0;
    if((str80[p] == 'X') && (str80[p+1] == ' '))
    {
      plus_patientcode = "";
      p += 2;
    }
    else
    {
      for(i=0; i<(80-p); i++)
      {
        if(str80[i+p] == ' ')
        {
          break;
        }
        str128[i] = str80[i+p];
        if(str128[i] == '_')  str128[i] = ' ';
      }
      try
      {
        plus_patientcode = tmp_str = new String(str128, 0, i, "US-ASCII");
      }
      catch(UnsupportedEncodingException e)
      {
        return -1;
      }
      p += i + 1;
    }

    if(str80[p] == 'M')
    {
      plus_gender = "Male";
    }
    else if(str80[p] == 'F')
      {
        plus_gender = "Female";
      }
      else
      {
        plus_gender = "";
      }
    p += 2;

    if(str80[p] == 'X')
    {
      plus_birthdate = "";
      p += 2;
    }
    else
    {
      for(i=0; i<(80-p); i++)
      {
        if(str80[i+p] == ' ')
        {
          break;
        }
        str128[i] = str80[i+p];
      }
      str128[2] = ' ';
      str128[3] += 32;
      str128[4] += 32;
      str128[5] += 32;
      str128[6] = ' ';
      p += i + 1;
      try
      {
        plus_birthdate = tmp_str = new String(str128, 0, 11, "US-ASCII");
      }
      catch(UnsupportedEncodingException e)
      {
        return -1;
      }
    }

    for(i=0; i<(80-p);i++)
    {
      if(str80[i+p] == ' ')
      {
        break;
      }
      str128[i] = str80[i+p];
      if(str128[i] == '_')  str128[i] = ' ';
    }
    p += i + 1;
    try
    {
      plus_patient_name = tmp_str = new String(str128, 0, i, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }

    for(i=0; i<(80-p);i++)
    {
      str128[i] = str80[i+p];
    }
    p += i + 1;
    try
    {
      plus_patient_additional = tmp_str = new String(str128, 0, i, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }
  }

/********************* EDF+ RECORDINGFIELD *********************************************/

  if((edfplus != 0) || (bdfplus != 0))
  {
    error = 0;
    p = 0;
    r = 0;
    for(i=0; i<80; i++)
    {
      str80[i] = hdr[i + 88];
    }
    if((str80[0] != 'S') ||
       (str80[1] != 't') ||
       (str80[2] != 'a') ||
       (str80[3] != 'r') ||
       (str80[4] != 't') ||
       (str80[5] != 'd') ||
       (str80[6] != 'a') ||
       (str80[7] != 't') ||
       (str80[8] != 'e') ||
       (str80[9] != ' '))
    {
      error = 1;
    }

    if(str80[10] == 'X')
    {
      if(str80[11] != ' ')  error = 1;
      if(str80[12] == ' ')  error = 1;
      p = 12;
    }
    else
    {
      if(str80[21]!=' ')  error = 1;
      if(str80[22]==' ')  error = 1;
      p = 22;
      for(i=0; i<11; i++)
      {
        str16[i] = str80[i + 10];
      }
      str16[11] = 0;
      if((str16[2]!='-')||(str16[6]!='-'))  error = 1;
      str16[2] = 0;
      str16[6] = 0;
      if((str16[0]<48)||(str16[0]>57))  error = 1;
      if((str16[1]<48)||(str16[1]>57))  error = 1;
      if((str16[7]<48)||(str16[7]>57))  error = 1;
      if((str16[8]<48)||(str16[8]>57))  error = 1;
      if((str16[9]<48)||(str16[9]>57))  error = 1;
      if((str16[10]<48)||(str16[10]>57))  error = 1;
      if((atof_nonlocalized(str16) < 1) || (atof_nonlocalized(str16) > 31))  error = 1;
      r = 0;
      if((str16[3] == 'J') && (str16[4] == 'A') && (str16[5] == 'N'))  r = 1;
        else if((str16[3] == 'F') && (str16[4] == 'E') && (str16[5] == 'B'))  r = 2;
          else if((str16[3] == 'M') && (str16[4] == 'A') && (str16[5] == 'R'))  r = 3;
            else if((str16[3] == 'A') && (str16[4] == 'P') && (str16[5] == 'R'))  r = 4;
              else if((str16[3] == 'M') && (str16[4] == 'A') && (str16[5] == 'Y'))  r = 5;
                else if((str16[3] == 'J') && (str16[4] == 'U') && (str16[5] == 'N'))  r = 6;
                  else if((str16[3] == 'J') && (str16[4] == 'U') && (str16[5] == 'L'))  r = 7;
                    else if((str16[3] == 'A') && (str16[4] == 'U') && (str16[5] == 'G'))  r = 8;
                      else if((str16[3] == 'S') && (str16[4] == 'E') && (str16[5] == 'P'))  r = 9;
                        else if((str16[3] == 'O') && (str16[4] == 'C') && (str16[5] == 'T'))  r = 10;
                          else if((str16[3] == 'N') && (str16[4] == 'O') && (str16[5] == 'V'))  r = 11;
                            else if((str16[3] == 'D') && (str16[4] == 'E') && (str16[5] == 'C'))  r = 12;
                              else error = 1;
    }

    n = 0;
    for(i=p; i<80; i++)
    {
      if(i > 78)
      {
        error = 1;
        break;
      }
      if(str80[i] == ' ')
      {
        n++;
        if(str80[i + 1] == ' ')
        {
          error = 1;
          break;
        }
      }
      if(n > 1)  break;
    }

    if(error != 0)  return -1;

    if(hdr[98] != 'X')
    {
      error = 0;

      for(j=0; j<8; j++)
      {
        str8[j] = hdr[168 + j];
      }
      str8[2] = 0;
      str8[5] = 0;

      if(atoi_nonlocalized(str8, 0) != atoi_nonlocalized(str16, 0))  error = 1;
      if(atoi_nonlocalized(str8, 3) != r)  error = 1;
      if(atoi_nonlocalized(str8, 6) != atoi_nonlocalized(str16, 9))  error = 1;
      if(error != 0)
      {
        return -1;
      }

      if(startdate_year != atoi_nonlocalized(str16, 7))
      {
        return -1;
      }
    }

    p = 10;
    for(i=0; i<(80-p); i++)
    {
      if(str80[i+p] == ' ')
      {
        break;
      }
      str128[i] = str80[i+p];
    }
    str128[2] = ' ';
    str128[3] += 32;
    str128[4] += 32;
    str128[5] += 32;
    str128[6] = ' ';
    p += i + 1;
    try
    {
      plus_startdate = tmp_str = new String(str128, 0, i, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }

    if((str80[p] == 'X') && (str80[p+1] == ' '))
    {
      plus_admincode = "";
      p += 2;
    }
    else
    {
      for(i=0; i<(80-p); i++)
      {
        if(str80[i+p] == ' ')
        {
          break;
        }
        str128[i] = str80[i+p];
        if(str128[i]=='_')  str128[i] = ' ';
      }
      p += i + 1;
      try
      {
        plus_admincode = tmp_str = new String(str128, 0, i, "US-ASCII");
      }
      catch(UnsupportedEncodingException e)
      {
        return -1;
      }
    }

    if((str80[p] == 'X') && (str80[p+1] == ' '))
    {
      plus_technician = "";
      p += 2;
    }
    else
    {
      for(i=0; i<(80-p); i++)
      {
        if(str80[i+p]==' ')
        {
          break;
        }
        str128[i] = str80[i+p];
        if(str128[i] == '_')  str128[i] = ' ';
      }
      p += i + 1;
      try
      {
        plus_technician = tmp_str = new String(str128, 0, i, "US-ASCII");
      }
      catch(UnsupportedEncodingException e)
      {
        return -1;
      }
    }

    if((str80[p] == 'X') && (str80[p+1] == ' '))
    {
      plus_equipment = "";
      p += 2;
    }
    else
    {
      for(i=0; i<(80-p); i++)
      {
        if(str80[i+p]==' ')
        {
          break;
        }
        str128[i] = str80[i+p];
        if(str128[i]=='_')  str128[i] = ' ';
      }
      p += i + 1;
      try
      {
        plus_equipment = tmp_str = new String(str128, 0, i, "US-ASCII");
      }
      catch(UnsupportedEncodingException e)
      {
        return -1;
      }
    }

    for(i=0; i<(80-p);i++)
    {
      str128[i] = str80[i+p];
    }
    p += i + 1;
    try
    {
      plus_recording_additional = tmp_str = new String(str128, 0, i, "US-ASCII");
    }
    catch(UnsupportedEncodingException e)
    {
      return -1;
    }
  }

/********************* FILESIZE *********************************************/

  hdrsize = edfsignals * 256 + 256;

  try
  {
    if(file_in.length() != ((long)recordsize * datarecords + (long)hdrsize))
    {
      return -1;
    }
  }
  catch(IOException e)
  {
    return -1;
  }

  param_buf_offset = new int[edfsignals];

  param_bitvalue = new double[edfsignals];

  param_offset = new double[edfsignals];

  n = 0;

  for(i=0; i<edfsignals; i++)
  {
    param_buf_offset[i] = n;
    if(bdf != 0)  n += param_smp_per_record[i] * 3;
    else  n += param_smp_per_record[i] * 2;

    param_bitvalue[i] = (param_phys_max[i] - param_phys_min[i]) / (param_dig_max[i] - param_dig_min[i]);
    param_offset[i] = param_phys_max[i] / param_bitvalue[i] - param_dig_max[i];
  }

  j = 0;

  for(i=0; i<edfsignals; i++)
  {
    if(param_annotation[i] == 0)
    {
      mapped_signals[j++] = i;
    }
  }

  param_sample_pntr = new long[edfsignals];

  return 0;
}

private int get_annotations(int read_annotations_mode) throws IOException
{
  int i, j, k, p, r=0, n,
      max,
      onset,
      duration,
      duration_start,
      zero,
      max_tal_ln=0,
      error,
      annots_in_record,
      annots_in_tal,
      samplesize=2;

  byte[] scratchpad,
         cnv_buf,
         time_in_txt,
         duration_in_txt,
         recording_ends_str = {'R','e','c','o','r','d','i','n','g',' ','e','n','d','s'};


  long data_record_duration = long_data_record_duration,
       elapsedtime,
       time_tmp=0;

  EDFAnnotationStruct new_annotation;

  if(bdfplus != 0)
  {
    samplesize = 3;
  }

  cnv_buf = new byte[recordsize];

  for(i=0; i<nr_annot_chns; i++)
  {
    if(max_tal_ln < param_smp_per_record[annot_ch[i]] * samplesize)  max_tal_ln = param_smp_per_record[annot_ch[i]] * samplesize;
  }

  if(max_tal_ln < 128)  max_tal_ln = 128;

  scratchpad = new byte[max_tal_ln + 3];

  time_in_txt = new byte[max_tal_ln + 3];

  duration_in_txt = new byte[max_tal_ln + 3];

  file_in.seek((edfsignals + 1) * 256);

  elapsedtime = 0;

  for(i=0; i<datarecords; i++)
  {
    file_in.read(cnv_buf);

/************** process annotationsignals (if any) **************/

    error = 0;

    for(r=0; r<nr_annot_chns; r++)
    {
      n = 0;
      zero = 0;
      onset = 0;
      duration = 0;
      duration_start = 0;
      scratchpad[0] = 0;
      annots_in_tal = 0;
      annots_in_record = 0;

      p = param_buf_offset[annot_ch[r]];
      max = param_smp_per_record[annot_ch[r]] * samplesize;

/************** process one annotation signal ****************/

      if(cnv_buf[p + max - 1] != 0)
      {
        return 5;
      }

      if(r == 0)  /* if it's the first annotation signal, then check */
      {           /* the timekeeping annotation */
        error = 1;

        for(k=0; k<(max-2); k++)
        {
          scratchpad[k] = cnv_buf[p + k];

          if(scratchpad[k] == 20)
          {
            if(cnv_buf[p + k + 1] != 20)
            {
              return 6;
            }
            scratchpad[k] = 0;
            if(is_onset_number(scratchpad) != 0)
            {
              return 36;
            }
            else
            {
              time_tmp = get_long_time(scratchpad);

              if(i != 0)
              {
                if(discontinuous != 0)
                {
                  if((time_tmp - elapsedtime) < data_record_duration)
                  {
                    return 4;
                  }
                }
                else
                {
                  if((time_tmp - elapsedtime) != data_record_duration)
                  {
                    return 3;
                  }
                }
              }
              else
              {
                if((time_tmp >= EDFLIB_TIME_DIMENSION) || (time_tmp < 0L))
                {
                  return 2;
                }
                else
                {
                  starttime_offset = time_tmp;
                  if(read_annotations_mode == EDFLIB_DO_NOT_READ_ANNOTATIONS)
                  {
                    return 0;
                  }
                }
              }
              elapsedtime = time_tmp;
              error = 0;
              break;
            }
          }
        }
      }

      for(k=0; k<max; k++)
      {
        scratchpad[n] = cnv_buf[p + k];

        if(scratchpad[n] == 0)
        {
          if(zero == 0)
          {
            if(k != 0)
            {
              if(cnv_buf[p + k - 1] != 20)
              {
                return 33;
              }
            }
            n = 0;
            onset = 0;
            duration = 0;
            duration_start = 0;
            scratchpad[0] = 0;
            annots_in_tal = 0;
          }
          zero++;
          continue;
        }
        if(zero > 1)
        {
          return 34;
        }
        zero = 0;

        if((scratchpad[n] == 20) || (scratchpad[n] == 21))
        {
          if(scratchpad[n] == 21)
          {
            if((duration != 0) || (duration_start != 0) || (onset != 0) || (annots_in_tal != 0))
            {              /* it's not allowed to have multiple duration fields */
              return 35;   /* in one TAL or to have a duration field which is   */
            }              /* not immediately behind the onsetfield             */
            duration_start = 1;
          }

          if((scratchpad[n] == 20) && (onset != 0) && (duration_start == 0))
          {
            if((r != 0) || (annots_in_record != 0))
            {
              if(annots_in_file >= annotlist_sz)
              {
                annotlist_sz += EDFLIB_ANNOT_MEMBLOCKSZ;

                annotationslist.ensureCapacity(annotlist_sz);
              }

              new_annotation = new EDFAnnotationStruct();

              if(duration != 0)
              {
                new_annotation.duration = (long)(atof_nonlocalized(duration_in_txt) * EDFLIB_TIME_DIMENSION);

                if(new_annotation.duration < -1L)
                {
                  new_annotation.duration = -1L;
                }
              }
              else
              {
                new_annotation.duration = -1L;
              }

              if(n > EDFLIB_MAX_ANNOTATION_LEN)  n = EDFLIB_MAX_ANNOTATION_LEN;

              new_annotation.description = new String(scratchpad, 0, n, "UTF-8");

              new_annotation.onset = get_long_time(time_in_txt);

              new_annotation.onset -= starttime_offset;

              annots_in_file++;

//               if(read_annotations_mode == EDFLIB_READ_ANNOTATIONS)
//               {
//                 if(strncmp(new_annotation->description, recording_ends_str, 14) == 0)
//                 {
//                   if(nr_annot_chns == 1)
//                   {
//                     return 0;
//                   }
//                 }
//               }

              annotationslist.add(new_annotation);
            }

            annots_in_tal++;
            annots_in_record++;
            n = 0;
            continue;
          }

          if(onset == 0)
          {
            scratchpad[n] = 0;
            if(is_onset_number(scratchpad) != 0)
            {
              return 36;
            }
            onset = 1;
            n = 0;
            strcpy(time_in_txt, scratchpad);
            continue;
          }

          if(duration_start != 0)
          {
            scratchpad[n] = 0;
            if(is_duration_number(scratchpad) != 0)
            {
              return 37;
            }

            for(j=0; j<n; j++)
            {
              if(j == 15)  break;
              duration_in_txt[j] = scratchpad[j];
              if((duration_in_txt[j] < 32) || (duration_in_txt[j] > 126))
              {
                duration_in_txt[j] = '.';
              }
            }
            duration_in_txt[j] = 0;

            duration = 1;
            duration_start = 0;
            n = 0;
            continue;
          }
        }

        n++;
      }
    }
  }

  return 0;
}

/* Checks a string for a valid integer number (left-aligned, filled-up with spaces, etc.) */
private int is_integer_number(byte[] str)
{
  int i=0, l, hasspace = 0, hassign=0, digit=0;

  l = str.length;

  if(l < 1)  return 1;

  if((str[0]=='+')||(str[0]=='-'))
  {
    hassign++;
    i++;
  }

  for(; i<l; i++)
  {
    if(str[i]==' ')
    {
      if(digit == 0)
      {
        return 1;
      }
      hasspace++;
    }
    else
    {
      if((str[i]<48)||(str[i]>57))
      {
        return 1;
      }
      else
      {
        if(hasspace > 0)
        {
          return 1;
        }
        digit++;
      }
    }
  }

  if(digit > 0)  return 0;
  else  return 1;
}

/* Checks a string for a valid (broken) number (left-aligned, filled-up with spaces, etc.) */
private int is_number(byte[] str)
{
  int i=0, l, hasspace = 0, hassign=0, digit=0, hasdot=0, hasexp=0;

  l = str.length;

  if(l < 1)  return 1;

  if((str[0]=='+')||(str[0]=='-'))
  {
    hassign++;
    i++;
  }

  for(; i<l; i++)
  {
    if((str[i]=='e')||(str[i]=='E'))
    {
      if((digit == 0)||(hasexp != 0))
      {
        return 1;
      }
      hasexp++;
      hassign = 0;
      digit = 0;

      break;
    }

    if(str[i]==' ')
    {
      if(digit == 0)
      {
        return 1;
      }
      hasspace++;
    }
    else
    {
      if(((str[i]<48)||(str[i]>57))&&str[i]!='.')
      {
        return 1;
      }
      else
      {
        if(hasspace != 0)
        {
          return 1;
        }
        if(str[i]=='.')
        {
          if(hasdot != 0)  return 1;
          hasdot++;
        }
        else
        {
          digit++;
        }
      }
    }
  }

  if(hasexp != 0)
  {
    if(++i==l)
    {
      return 1;
    }

    if((str[i]=='+')||(str[i]=='-'))
    {
      hassign++;
      i++;
    }

    for(; i<l; i++)
    {
      if(str[i]==' ')
      {
        if(digit == 0)
        {
          return 1;
        }
        hasspace++;
      }
      else
      {
        if((str[i]<48)||(str[i]>57))
        {
          return 1;
        }
        else
        {
          if(hasspace != 0)
          {
            return 1;
          }

          digit++;
        }
      }
    }
  }

  if(digit != 0)  return 0;
  else  return 1;
}

/* Get long duration from a string, used for the header only */
private long get_long_duration_hdr(byte[] str)
{
  int i, len=8, hasdot=0, dotposition=0;

  long value=0, radix;

  if((str[0] == '+') || (str[0] == '-'))
  {
    for(i=0; i<7; i++)
    {
      str[i] = str[i+1];
    }

    str[7] = ' ';
  }

  for(i=0; i<8; i++)
  {
    if(str[i] == ' ')
    {
      len = i;
      break;
    }
  }

  for(i=0; i<len; i++)
  {
    if(str[i]=='.')
    {
      hasdot = 1;
      dotposition = i;
      break;
    }
  }

  if(hasdot != 0)
  {
    radix = EDFLIB_TIME_DIMENSION;

    for(i=dotposition-1; i>=0; i--)
    {
        value += ((long)(str[i] - 48)) * radix;
        radix *= 10;
    }

    radix = EDFLIB_TIME_DIMENSION / 10;

    for(i=dotposition+1; i<len; i++)
    {
        value += ((long)(str[i] - 48)) * radix;
        radix /= 10;
    }
  }
  else
  {
    radix = EDFLIB_TIME_DIMENSION;

    for(i=len-1; i>=0; i--)
    {
        value += ((long)(str[i] - 48)) * radix;
        radix *= 10;
    }
  }

  return value;
}

/* Converts a number in ASCII to integer */
private int atoi_nonlocalized(byte[] str, int i)
{
  int len, value=0, sign=1;

  if(i < 0)  return 0;

  len = str.length;

  if(len < 1)  return 0;

  if(i >= len)  return 0;

  while(str[i] == ' ')
  {
    i++;
  }

  if(i >= len)  return 0;

  if((str[i] == '+') || (str[i] == '-'))
  {
    if(str[i] == '-')
    {
      sign = -1;
    }

    i++;
  }

  if(i >= len)  return 0;

  for( ; i<len; i++)
  {
    if((str[i] < '0') || (str[i] > '9'))
    {
      break;
    }

    value *= 10;

    value += ((str[i] - '0') * sign);
  }

  return value;
}

/* Converts a (broken) number in ASCII to double */
private double atof_nonlocalized(byte[] str)
{
  int len, i=0, j, dot_pos=-1, decimals=0, sign=1, exp_pos=-1, exp_sign=1, exp_val=0;

  double value, value2=0;

  len = str.length;

  if(len < 1)  return 0;

  value = atoi_nonlocalized(str, 0);

  while(str[i] == ' ')
  {
    i++;
  }

  if((str[i] == '+') || (str[i] == '-'))
  {
    if(str[i] == '-')
    {
      sign = -1;
    }

    i++;
  }

  for(; i<len; i++)
  {
    if((str[i] == 'e') || (str[i] == 'E'))
    {
      exp_pos = i;

      break;
    }

    if(((str[i] < '0') || (str[i] > '9')) && (str[i] != '.'))
    {
      break;
    }

    if(dot_pos >= 0)
    {
      if((str[i] >= '0') && (str[i] <= '9'))
      {
        decimals++;
      }
      else
      {
        break;
      }
    }

    if(str[i] == '.')
    {
      if(dot_pos < 0)
      {
        dot_pos = i;
      }
    }
  }

  if(decimals != 0)
  {
    value2 = atoi_nonlocalized(str, dot_pos + 1) * sign;

    i = 1;

    while(0 != decimals--)
    {
      i *= 10;
    }

    value2 /= i;

    value += value2;
  }

  if(exp_pos > 0)
  {
    i = exp_pos + 1;

    if(i<len)
    {
      if(str[i] == '+')
      {
        i++;
      }
      else if(str[i] == '-')
        {
          exp_sign = -1;

          i++;
        }

      if(i<len)
      {
        exp_val = atoi_nonlocalized(str, i);

        if(exp_val > 0)
        {
          for(j=0; j<exp_val; j++)
          {
            if(exp_sign > 0)
            {
              value *= 10;
            }
            else
            {
              value /= 10;
            }
          }
        }
      }
    }
  }

  return value;
}


private int is_duration_number(byte[] str)
{
  int i, l, hasdot = 0;

  l = str.length;

  if(l < 1)  return 1;

  if((str[0] == '.') || (str[l-1] == '.'))  return 1;

  for(i=0; i<l; i++)
  {
    if(str[i] == 0)
    {
      if(i < 1)
      {
        return 1;
      }
      else
      {
        return 0;
      }
    }

    if(str[i] == '.')
    {
      if(hasdot != 0)  return 1;
      hasdot++;
    }
    else
    {
      if((str[i] < '0') || (str[i] > '9'))  return 1;
    }
  }

  return 0;
}


private int is_onset_number(byte[] str)
{
  int i, l, hasdot = 0;

  l = str.length;

  if(l <2 )  return 1;

  if((str[0] != '+') && (str[0] != '-'))  return 1;

  if((str[1] == '.') || (str[l-1] == '.'))  return 1;

  for(i=1; i<l; i++)
  {
    if(str[i] == 0)
    {
      if(i < 2)
      {
        return 1;
      }
      else
      {
        return 0;
      }
    }

    if(str[i] == '.')
    {
      if(hasdot != 0)  return 1;
      hasdot++;
    }
    else
    {
      if((str[i] < '0') || (str[i] > '9'))  return 1;
    }
  }

  return 0;
}


private long get_long_time(byte[] str)
{
  int i=0, len, hasdot=0, dotposition=0, neg=0;

  long value=0, radix;

  len = strlen(str);

  if(len < 1)  return 0;

  if(str[0] == '+')
  {
    i++;
  }
  else if(str[0] == '-')
    {
      neg = 1;
      i++;
    }

  for(; i<len; i++)
  {
    if(str[i]=='.')
    {
      hasdot = 1;
      dotposition = i;
      break;
    }
  }

  if(hasdot != 0)
  {
    radix = EDFLIB_TIME_DIMENSION;

    for(i=dotposition-1; i>=1; i--)
    {
        value += ((long)(str[i] - 48)) * radix;
        radix *= 10;
    }

    radix = EDFLIB_TIME_DIMENSION / 10;

    for(i=dotposition+1; i<len; i++)
    {
        value += ((long)(str[i] - 48)) * radix;
        radix /= 10;
    }
  }
  else
  {
    radix = EDFLIB_TIME_DIMENSION;

    for(i=len-1; i>=1; i--)
    {
        value += ((long)(str[i] - 48)) * radix;
        radix *= 10;
    }
  }

  if(neg != 0)  value = -value;

  return value;
}

private int strcpy(byte[] dest, byte[] src)
{
  int i, sz, srclen;

  sz = dest.length - 1;

  srclen = strlen(src);

  if(srclen > sz)  srclen = sz;

  if(srclen < 0)  return 0;

  for(i=0; i<srclen; i++)
  {
    dest[i] = src[i];
  }

  dest[srclen] = 0;

  return srclen;
}

private int strlen(byte[] str)
{
  int i;

  for(i=0; i<str.length; i++)
  {
    if(str[i] == 0)
    {
      return i;
    }
  }

  return i;
}

private int strncmp(byte[] str1, byte[] str2, int len)
{
  int i;

  if(len > str1.length)  len = str1.length;

  if(len > str2.length)  len = str2.length;

  for(i=0; i<len; i++)
  {
    if(str1[i] != str2[i])  return 1;

    if(str1[i] == 0)  return 0;
  }

  return 0;
}

}
















