package ai.gsmc.fundamentals.material;

/**
 * What kind of thing a {@link Material} is. Drives default forms and recipe behaviour.
 * Append-only shared schema (PLAN §4).
 */
public enum MaterialType {
    /** A chemical element, e.g. iron, neodymium. */
    ELEMENT,
    /** A defined chemical compound, e.g. tungsten trioxide, alumina. */
    COMPOUND,
    /** A metallic alloy, e.g. steel, ferrochrome, NdFeB. */
    ALLOY,
    /** A naturally occurring ore mineral, e.g. chalcopyrite, bastnäsite. */
    MINERAL,
    /** A beneficiation product (upgraded ore), e.g. a mixed rare-earth concentrate. */
    CONCENTRATE
}
