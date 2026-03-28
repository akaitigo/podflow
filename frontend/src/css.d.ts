/** Ambient module declaration for CSS Modules. */
declare module "*.module.css" {
	const classes: Readonly<Record<string, string>>;
	export default classes;
}
