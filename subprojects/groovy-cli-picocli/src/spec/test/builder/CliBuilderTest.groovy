/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package builder

import groovy.cli.picocli.CliBuilder
import groovy.cli.Option
import groovy.cli.TypedOption
import groovy.cli.Unparsed
import groovy.transform.TypeChecked

import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import static java.util.concurrent.TimeUnit.DAYS
import static java.util.concurrent.TimeUnit.HOURS

//import java.math.RoundingMode

class CliBuilderTest extends GroovyTestCase {
//    void tearDown() {
//    }

    // tag::annotationInterfaceSpec[]
    interface GreeterI {
        @Option(shortName='h', description='display usage') Boolean help()        // <1>
        @Option(shortName='a', description='greeting audience') String audience() // <2>
        @Unparsed List remaining()                                                // <3>
    }
    // end::annotationInterfaceSpec[]

    // tag::annotationClassSpec[]
    class GreeterC {
        @Option(shortName='h', description='display usage')
        Boolean help                        // <1>

        private String audience
        @Option(shortName='a', description='greeting audience')
        void setAudience(String audience) { // <2>
            this.audience = audience
        }
        String getAudience() { audience }

        @Unparsed
        List remaining                      // <3>
    }
    // end::annotationClassSpec[]

    void testAnnotationsInterface() {
        // tag::annotationInterface[]
        def cli = new CliBuilder(usage: 'groovy Greeter [option]')  // <1>
        def argz = '--audience Groovologist'.split()
        def options = cli.parseFromSpec(GreeterI, argz)             // <2>
        assert options.audience() == 'Groovologist'                 // <3>

        argz = '-h Some Other Args'.split()
        options = cli.parseFromSpec(GreeterI, argz)                 // <4>
        assert options.help()
        assert options.remaining() == ['Some', 'Other', 'Args']     // <5>
        // end::annotationInterface[]
    }

    void testAnnotationsClass() {
        // tag::annotationClass[]
        def cli = new CliBuilder(usage: 'groovy Greeter [option]') // <1>
        def options = new GreeterC()                               // <2>
        def argz = '--audience Groovologist foo'.split()
        cli.parseFromInstance(options, argz)                       // <3>
        assert options.audience == 'Groovologist'                  // <4>
        assert options.remaining == ['foo']                        // <5>
        // end::annotationClass[]
    }

    void testParseScript() {
        def argz = '--audience Groovologist foo'.split()
        new GroovyShell().run('''
            // tag::annotationScript[]
            import groovy.cli.picocli.CliBuilder
            import groovy.cli.OptionField
            import groovy.cli.UnparsedField

            @OptionField String audience
            @OptionField Boolean help
            @UnparsedField List remaining
            new CliBuilder().parseFromInstance(this, args)
            assert audience == 'Groovologist'
            assert remaining == ['foo']
            // end::annotationScript[]
        ''', 'TestScript.groovy', argz)
    }

    void testWithArgument() {
        // tag::withArgument[]
        def cli = new CliBuilder()
        cli.a(args: 0, 'a arg') // <1>
        cli.b(args: 1, 'b arg') // <2>
        cli.c(args: 1, optionalArg: true, 'c arg') // <3>
        def options = cli.parse('-a -b foo -c bar baz'.split()) // <4>

        assert options.a == true
        assert options.b == 'foo'
        assert options.c == 'bar'
        assert options.arguments() == ['baz']

        options = cli.parse('-a -c -b foo bar baz'.split()) // <5>

        assert options.a == true
        assert options.c == true
        assert options.b == 'foo'
        assert options.arguments() == ['bar', 'baz']
        // end::withArgument[]
    }

    // tag::withArgumentInterfaceSpec[]
    interface WithArgsI {
        @Option boolean a()
        @Option String b()
        @Option(optionalArg=true) String[] c()
        @Unparsed List remaining()
    }
    // end::withArgumentInterfaceSpec[]

    void testWithArgumentInterface() {
        // tag::withArgumentInterface[]
        def cli = new CliBuilder()
        def options = cli.parseFromSpec(WithArgsI, '-a -b foo -c bar baz'.split())
        assert options.a()
        assert options.b() == 'foo'
        assert options.c() == ['bar']
        assert options.remaining() == ['baz']

        options = cli.parseFromSpec(WithArgsI, '-a -c -b foo bar baz'.split())
        assert options.a()
        assert options.c() == []
        assert options.b() == 'foo'
        assert options.remaining() == ['bar', 'baz']
        // end::withArgumentInterface[]
    }

    void testMultipleArgsAndOptionalValueSeparator() {
        // tag::multipleArgs[]
        def cli = new CliBuilder()
        cli.a(args: 2, 'a-arg')
        cli.b(args: '2', valueSeparator: ',', 'b-arg') // <1>
        cli.c(args: '+', valueSeparator: ',', 'c-arg') // <2>

        def options = cli.parse('-a 1 2 3 4'.split()) // <3>
        assert options.a == '1' // <4>
        assert options.as == ['1', '2'] // <5>
        assert options.arguments() == ['3', '4']

        options = cli.parse('-a1 -a2 3'.split()) // <6>
        assert options.as == ['1', '2']
        assert options.arguments() == ['3']

        options = cli.parse(['-b1,2']) // <7>
        assert options.bs == ['1', '2']

        options = cli.parse(['-c', '1'])
        assert options.cs == ['1']

        options = cli.parse(['-c1'])
        assert options.cs == ['1']

        options = cli.parse(['-c1,2,3'])
        assert options.cs == ['1', '2', '3']
        // end::multipleArgs[]
    }

    // tag::multipleArgsInterfaceSpec[]
    interface ValSepI {
        @Option(numberOfArguments=2) String[] a()
        @Option(numberOfArgumentsString='2', valueSeparator=',') String[] b()
        @Option(numberOfArgumentsString='+', valueSeparator=',') String[] c()
        @Unparsed remaining()
    }
    // end::multipleArgsInterfaceSpec[]

    void testMultipleArgsAndOptionalValueSeparatorInterface() {
        // tag::multipleArgsInterface[]
        def cli = new CliBuilder()

        def options = cli.parseFromSpec(ValSepI, '-a 1 2 3 4'.split())
        assert options.a() == ['1', '2']
        assert options.remaining() == ['3', '4']

        options = cli.parseFromSpec(ValSepI, '-a1 -a2 3'.split())
        assert options.a() == ['1', '2']
        assert options.remaining() == ['3']

        options = cli.parseFromSpec(ValSepI, ['-b1,2'] as String[])
        assert options.b() == ['1', '2']

        options = cli.parseFromSpec(ValSepI, ['-c', '1'] as String[])
        assert options.c() == ['1']

        options = cli.parseFromSpec(ValSepI, ['-c1'] as String[])
        assert options.c() == ['1']

        options = cli.parseFromSpec(ValSepI, ['-c1,2,3'] as String[])
        assert options.c() == ['1', '2', '3']
        // end::multipleArgsInterface[]
    }

    void testType() {
        // tag::withType[]
        def argz = '''-a John -b -d 21 -e 1980 -f 3.5 -g 3.14159
            -h cv.txt -i DOWN and some more'''.split()
        def cli = new CliBuilder()
        cli.a(type: String, 'a-arg')
        cli.b(type: boolean, 'b-arg')
        cli.c(type: Boolean, 'c-arg')
        cli.d(type: int, 'd-arg')
        cli.e(type: Long, 'e-arg')
        cli.f(type: Float, 'f-arg')
        cli.g(type: BigDecimal, 'g-arg')
        cli.h(type: File, 'h-arg')
        cli.i(type: RoundingMode, 'i-arg')
        def options = cli.parse(argz)
        assert options.a == 'John'
        assert options.b
        assert !options.c
        assert options.d == 21
        assert options.e == 1980L
        assert options.f == 3.5f
        assert options.g == 3.14159
        assert options.h == new File('cv.txt')
        assert options.i == RoundingMode.DOWN
        assert options.arguments() == ['and', 'some', 'more']
        // end::withType[]
    }

    void testTypeMultiple() {
        // tag::withTypeMultiple[]
        def argz = '''-j 3 4 5 -k1.5,2.5,3.5 and some more'''.split()
        def cli = new CliBuilder()
        cli.j(args: 3, type: int[], 'j-arg')
        cli.k(args: '+', valueSeparator: ',', type: BigDecimal[], 'k-arg')
        def options = cli.parse(argz)
        assert options.js == [3, 4, 5] // <1>
        assert options.j == [3, 4, 5]  // <1>
        assert options.k == [1.5, 2.5, 3.5]
        assert options.arguments() == ['and', 'some', 'more']
        // end::withTypeMultiple[]
    }

    void testConvert() {
        // tag::withConvert[]
        def argz = '''-a John -b Mary -d 2016-01-01 and some more'''.split()
        def cli = new CliBuilder()
        def lower = { it.toLowerCase() }
        cli.a(convert: lower, 'a-arg')
        cli.b(convert: { it.toUpperCase() }, 'b-arg')
        cli.d(convert: { new SimpleDateFormat("yyyy-MM-dd").parse(it) }, 'd-arg')
        def options = cli.parse(argz)
        assert options.a == 'john'
        assert options.b == 'MARY'
        assert new SimpleDateFormat("dd-MMM-yyyy").format(options.d) == '01-Jan-2016'
        assert options.arguments() == ['and', 'some', 'more']
        // end::withConvert[]
    }

    // tag::withConvertInterfaceSpec[]
    interface WithConvertI {
        @Option(convert={ it.toLowerCase() }) String a()
        @Option(convert={ it.toUpperCase() }) String b()
        @Option(convert={ new SimpleDateFormat("yyyy-MM-dd").parse(it) }) Date d()
        @Unparsed List remaining()
    }
    // end::withConvertInterfaceSpec[]

    void testConvertInterface() {
        // tag::withConvertInterface[]
        Date newYears = new SimpleDateFormat("yyyy-MM-dd").parse("2016-01-01")
        def argz = '''-a John -b Mary -d 2016-01-01 and some more'''.split()
        def cli = new CliBuilder()
        def options = cli.parseFromSpec(WithConvertI, argz)
        assert options.a() == 'john'
        assert options.b() == 'MARY'
        assert options.d() == newYears
        assert options.remaining() == ['and', 'some', 'more']
        // end::withConvertInterface[]
    }

    void testDefaultValue() {
        // tag::withDefaultValue[]
        def cli = new CliBuilder()
        cli.f longOpt: 'from', type: String, args: 1, defaultValue: 'one', 'f option'
        cli.t longOpt: 'to', type: int, defaultValue: '35', 't option'

        def options = cli.parse('-f two'.split())
        assert options.hasOption('f')
        assert options.f == 'two'
        assert !options.hasOption('t')
        assert options.t == 35

        options = cli.parse('-t 45'.split())
        assert !options.hasOption('from')
        assert options.from == 'one'
        assert options.hasOption('to')
        assert options.to == 45
        // end::withDefaultValue[]
    }

    // tag::withDefaultValueInterfaceSpec[]
    interface WithDefaultValueI {
        @Option(shortName='f', defaultValue='one') String from()
        @Option(shortName='t', defaultValue='35') int to()
    }
    // end::withDefaultValueInterfaceSpec[]

    void testDefaultValueInterface() {
        // tag::withDefaultValueInterface[]
        def cli = new CliBuilder()

        def options = cli.parseFromSpec(WithDefaultValueI, '-f two'.split())
        assert options.from() == 'two'
        assert options.to() == 35

        options = cli.parseFromSpec(WithDefaultValueI, '-t 45'.split())
        assert options.from() == 'one'
        assert options.to() == 45
        // end::withDefaultValueInterface[]
    }

    // tag::withTypeCheckedInterfaceSpec[]
    interface TypeCheckedI{
        @Option String name()
        @Option int age()
        @Unparsed List remaining()
    }
    // end::withTypeCheckedInterfaceSpec[]

    // tag::withTypeCheckedInterface[]
    @TypeChecked
    void testTypeCheckedInterface() {
        def argz = "--name John --age 21 and some more".split()
        def cli = new CliBuilder()
        def options = cli.parseFromSpec(TypeCheckedI, argz)
        String n = options.name()
        int a = options.age()
        assert n == 'John' && a == 21
        assert options.remaining() == ['and', 'some', 'more']
    }
    // end::withTypeCheckedInterface[]

    // tag::withTypeChecked[]
    @TypeChecked
    void testTypeChecked() {
        def cli = new CliBuilder(acceptLongOptionsWithSingleHyphen: true)
        TypedOption<String> name = cli.option(String, opt: 'n', longOpt: 'name', 'name option')
        TypedOption<Integer> age = cli.option(Integer, longOpt: 'age', 'age option')
        def argz = "--name John -age 21 and some more".split()
        def options = cli.parse(argz)
        String n = options[name]
        int a = options[age]
        assert n == 'John' && a == 21
        assert options.arguments() == ['and', 'some', 'more']
    }
    // end::withTypeChecked[]

    @TypeChecked
    void testTypeChecked_defaultOnlyDoubleHyphen() {
        def cli = new CliBuilder()
        TypedOption<String> name = cli.option(String, opt: 'n', longOpt: 'name', 'name option')
        TypedOption<Integer> age = cli.option(Integer, longOpt: 'age', 'age option')
        def argz = "--name John -age 21 and some more".split()
        def options = cli.parse(argz)
        assert options[name] == 'John'
        assert options[age] == null
        assert options.arguments() == ['-age', '21', 'and', 'some', 'more']
    }

    void testUsageMessageSpec() {
        // suppress ANSI escape codes to make this test pass on all environments
        System.setProperty("picocli.ansi", "false")
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        System.setOut(new PrintStream(baos, true))

        // tag::withUsageMessageSpec[]
        def cli = new CliBuilder()
        cli.name = "myapp"
        cli.usageMessage.with {
            headerHeading("@|bold,underline Header heading:|@%n")
            header("Header 1", "Header 2")                     // before the synopsis
            synopsisHeading("%n@|bold,underline Usage:|@ ")
            descriptionHeading("%n@|bold,underline Description heading:|@%n")
            description("Description 1", "Description 2")      // after the synopsis
            optionListHeading("%n@|bold,underline Options heading:|@%n")
            footerHeading("%n@|bold,underline Footer heading:|@%n")
            footer("Footer 1", "Footer 2")
        }
        cli.a('option a description')
        cli.b('option b description')
        cli.c(args: '*', 'option c description')
        cli.usage()
        // end::withUsageMessageSpec[]

        String expected = '''\
Header heading:
Header 1
Header 2

Usage: myapp [-ab] [-c[=PARAM...]]...

Description heading:
Description 1
Description 2

Options heading:
  -a               option a description
  -b               option b description
  -c= [PARAM...]   option c description

Footer heading:
Footer 1
Footer 2
'''
        assertEquals(expected.normalize(), baos.toString().normalize())
    }

    public void testMapOption() {
        // tag::MapOption[]
        def cli = new CliBuilder()
        cli.D(args: 2,   valueSeparator: '=', 'the old way')                          // <1>
        cli.X(type: Map, 'the new way')                                               // <2>
        cli.Z(type: Map, auxiliaryTypes: [TimeUnit, Integer].toArray(), 'typed map')  // <3>

        def options = cli.parse('-Da=b -Dc=d -Xx=y -Xi=j -ZDAYS=2 -ZHOURS=23'.split())// <4>
        assert options.Ds == ['a', 'b', 'c', 'd']                                     // <5>
        assert options.Xs == [ 'x':'y', 'i':'j' ]                                     // <6>
        assert options.Zs == [ (DAYS as TimeUnit):2, (HOURS as TimeUnit):23 ]         // <7>
        // end::MapOption[]
    }

    public void testGroovyDocAntExample() {
        def cli = new CliBuilder(usage:'ant [options] [targets]',
                header:'Options:')
        cli.help('print this message')
        cli.logfile(type:File, argName:'file', 'use given file for log')
        cli.D(type:Map, argName:'property=value', 'use value for given property')
        cli.lib(argName:'path', valueSeparator:',', args: '3',
                'comma-separated list of 3 paths to search for jars and classes')

        // suppress ANSI escape codes to make this test pass on all environments
        System.setProperty("picocli.ansi", "false")
        StringWriter sw = new StringWriter()
        cli.writer = new PrintWriter(sw)

        cli.usage()

        String expected = '''\
Usage: ant [options] [targets]
Options:
  -D= <property=value>   use value for given property
      -help              print this message
      -lib=<path>,<path>,<path>
                         comma-separated list of 3 paths to search for jars and
                           classes
      -logfile=<file>    use given file for log
'''
        assertEquals(expected.normalize(), sw.toString().normalize())
    }

    public void testGroovyDocCurlExample() {
        // suppress ANSI escape codes to make this test pass on all environments
        System.setProperty("picocli.ansi", "false")
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        System.setOut(new PrintStream(baos, true))

        def cli = new CliBuilder(name:'curl')
        cli._(longOpt:'basic', 'Use HTTP Basic Authentication')
        cli.d(longOpt:'data', args:1, argName:'data', 'HTTP POST data')
        cli.G(longOpt:'get', 'Send the -d data with a HTTP GET')
        cli.q('If used as the first parameter disables .curlrc')
        cli._(longOpt:'url', type:URL, argName:'URL', 'Set URL to work with')

        cli.usageMessage.sortOptions(false)
        cli.usage()

        String expected = '''\
Usage: curl [-Gq] [--basic] [--url=<URL>] [-d=<data>]
      --basic         Use HTTP Basic Authentication
  -d, --data=<data>   HTTP POST data
  -G, --get           Send the -d data with a HTTP GET
  -q                  If used as the first parameter disables .curlrc
      --url=<URL>     Set URL to work with
'''
        assertEquals(expected.normalize(), baos.toString().normalize())
    }
}
