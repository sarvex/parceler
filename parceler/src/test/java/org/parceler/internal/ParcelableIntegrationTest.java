/**
 * Copyright 2011-2015 John Ericksen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.parceler.internal;

import android.os.Parcel;
import android.os.Parcelable;
import org.androidtransfuse.adapter.ASTType;
import org.androidtransfuse.adapter.classes.ASTClassFactory;
import org.androidtransfuse.bootstrap.Bootstrap;
import org.androidtransfuse.bootstrap.Bootstraps;
import org.androidtransfuse.gen.ClassNamer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.parceler.Parcels;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;

/**
 * @author John Ericksen
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
@Bootstrap
public class ParcelableIntegrationTest {

    private static final String TEST_VALUE = "test value";

    @Inject
    private ASTClassFactory astClassFactory;
    @Inject
    private CodeGenerationUtil codeGenerationUtil;
    @Inject
    private ParcelableGenerator parcelableGenerator;
    @Inject
    private ParcelableAnalysis parcelableAnalysis;

    private Parcel parcel;
    private Class<Parcelable> parcelableClass;

    @Before
    public void setup() throws ClassNotFoundException, IOException, NoSuchFieldException, IllegalAccessException {
        Bootstraps.inject(this);

        ASTType mockParcelASTType = astClassFactory.getType(ParcelTarget.class);
        ASTType mockParcelTwoASTType = astClassFactory.getType(ParcelSecondTarget.class);

        ParcelableDescriptor parcelableDescriptor = parcelableAnalysis.analyze(mockParcelASTType);
        ParcelableDescriptor parcelableTwoDescriptor = parcelableAnalysis.analyze(mockParcelTwoASTType);

        parcelableGenerator.generateParcelable(mockParcelASTType, parcelableDescriptor);
        parcelableGenerator.generateParcelable(mockParcelTwoASTType, parcelableTwoDescriptor);

        ClassLoader classLoader = codeGenerationUtil.build();

        parcelableClass = (Class<Parcelable>) classLoader.loadClass(ClassNamer.className(mockParcelASTType).append(Parcels.IMPL_EXT).build().toString());

        parcel = Parcel.obtain();
    }

    @Test
    public void testGeneratedParcelable() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchFieldException {

        ParcelTarget parcelTarget = new ParcelTarget();
        ParcelSecondTarget parcelSecondTarget = new ParcelSecondTarget();

        parcelTarget.setDoubleValue(Math.PI);
        parcelTarget.setStringValue(TEST_VALUE);
        parcelTarget.setSecondTarget(parcelSecondTarget);

        parcelSecondTarget.setValue(TEST_VALUE);

        Parcelable outputParcelable = parcelableClass.getConstructor(ParcelTarget.class).newInstance(parcelTarget);

        outputParcelable.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Parcelable inputParcelable = ((Parcelable.Creator<Parcelable>)parcelableClass.getField("CREATOR").get(null)).createFromParcel(parcel);

        ParcelTarget wrapped = Parcels.unwrap(inputParcelable);

        assertEquals(parcelTarget, wrapped);
    }
}
