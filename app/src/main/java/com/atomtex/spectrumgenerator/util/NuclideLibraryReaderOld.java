package com.atomtex.spectrumgenerator.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import com.atomtex.spectrumgenerator.domain.EnergyLine;
import com.atomtex.spectrumgenerator.domain.Nuclide;
import com.atomtex.spectrumgenerator.util.Encrypter;
import com.atomtex.spectrumgenerator.util.NuclideLibraryException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static android.support.constraint.Constraints.TAG;

public final class NuclideLibraryReaderOld {

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static List<Nuclide> getLibrary(File file) throws NuclideLibraryException {
        List<Nuclide> list = new ArrayList<>();
        List<Charset> charsets = new ArrayList<>();

        //The charsets to parse data
        charsets.add(Charset.forName("UTF-16LE"));
//        charsets.add(Charset.forName("windows-1251"));
//        charsets.add(Charset.forName("UTF-16LE"));

        byte[] data;
        try (InputStream stream = new FileInputStream(file)) {
            data = new byte[stream.available()];
            int n = stream.read(data);
            if (n == 0) {
                throw new NuclideLibraryException("The nuclide library is empty");
            }
            data = Encrypter.convertData(data);
        } catch (IOException e) {
            throw new NuclideLibraryException();
        }


        for (Charset charset : charsets) {

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(data), charset))) {
                Log.e(TAG, "***************************getLibrary: ");

//            int nuclNum = Integer.parseInt(reader.readLine().trim());
                int nuclNum = Integer.parseInt(reader.readLine());
                for (int i = 0; i < nuclNum; i++) {
                    Nuclide nuclide = new Nuclide();
                    String[] string = reader.readLine().trim().split("\t");
                    String[] id = string[0].trim().split("-");
                    nuclide.setCategory(Nuclide.Category.valueOf(id[0]));
                    String name = id[1];
                    String num = id[2];
                    nuclide.setName(name);
                    nuclide.setNumStr(num);
                    String res = name.toUpperCase() + "_" + num;
//                    nuclide.setSound(SoundEvent.valueOf(res));
                    int linesNum = Integer.parseInt(string[1]);
                    nuclide.setLinesNum(linesNum);

                    EnergyLine[] lines = new EnergyLine[linesNum];

                    int appender = 0;
                    for (int j = 0; j < linesNum; j++) {
                        lines[j] = new EnergyLine(
                                Float.parseFloat(string[2 + appender]),
                                Integer.parseInt(string[3 + appender]),
                                Integer.parseInt(string[4 + appender]),
                                Integer.parseInt(string[5 + appender]),
                                Integer.parseInt(string[6 + appender]));
                        appender += 5;
                    }
                    nuclide.setEnergyLines(lines);
                    list.add(nuclide);
                }
            } catch (Exception e) {
                list.clear();
                continue;
            }
            return list;
        }
        throw new NuclideLibraryException("Unable to parse data from nuclide library");
    }
}
