package dev.pactgen.verification;

/**
 * Expresiones de los estados del proveedor que el arnés de piggymetrics reconoce para simular los errores que
 * dependen de los datos del servicio (se aplican con {@link ContractFixtures.Expected#stateMatches}, sin distinguir
 * mayúsculas). Deben coincidir con la tabla «Convención de redacción de los estados del proveedor» de
 * experiment/PROTOCOLO_SESION.md: lo comprueba {@code ProviderStatesTest}.
 *
 * <p>Solo se usan cuando el contrato espera el código de error correspondiente (400 en account-service, 500 en
 * auth-service); en cualquier otro caso, el estado no influye.
 */
public final class ProviderStates {

    private ProviderStates() {}

    /** «La cuenta / el usuario ya existe»: Assert.isNull en AccountServiceImpl.create; IllegalArgumentException en UserServiceImpl.create. */
    public static final String ALREADY_EXISTS = "ya existe|\\bexistente|duplicad|already exist|ya est[aá] registrad|en uso";

    /** «La cuenta no existe»: Assert.notNull en AccountServiceImpl.saveChanges. */
    public static final String NOT_FOUND = "no existe|inexistente|not exist|not found|no encontrad|no est[aá] registrad|sin cuenta";
}
